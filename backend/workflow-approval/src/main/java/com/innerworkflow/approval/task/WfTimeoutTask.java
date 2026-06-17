package com.innerworkflow.approval.task;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.innerworkflow.approval.entity.WfApprovalTask;
import com.innerworkflow.approval.entity.WfProcessInstance;
import com.innerworkflow.approval.entity.WfTimeoutRemind;
import com.innerworkflow.approval.service.WfApprovalTaskService;
import com.innerworkflow.approval.service.WfProcessInstanceService;
import com.innerworkflow.approval.service.WfTimeoutRemindService;
import com.innerworkflow.auth.entity.SysUser;
import com.innerworkflow.auth.service.SysUserService;
import com.innerworkflow.bpmn.entity.WfNodeConfig;
import com.innerworkflow.bpmn.service.WfNodeConfigService;
import com.innerworkflow.common.enums.TaskStatusEnum;
import com.innerworkflow.common.util.SpringContextHolder;
import com.innerworkflow.notify.dto.NotifySendDTO;
import com.innerworkflow.notify.enums.EventTypeEnum;
import com.innerworkflow.notify.service.WfNotifyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WfTimeoutTask {

    private final WfApprovalTaskService approvalTaskService;
    private final WfTimeoutRemindService timeoutRemindService;
    private final WfProcessInstanceService processInstanceService;
    private final WfNodeConfigService nodeConfigService;
    private final WfNotifyService notifyService;
    private final RuntimeService runtimeService;
    private final TaskService taskService;

    @Scheduled(cron = "0 */5 * * * ?")
    public void processTimeoutTasks() {
        log.info("开始处理超时任务...");

        try {
            LambdaQueryWrapper<WfApprovalTask> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(WfApprovalTask::getTaskStatus, TaskStatusEnum.PENDING.getCode());
            wrapper.isNotNull(WfApprovalTask::getDueTime);
            wrapper.lt(WfApprovalTask::getDueTime, LocalDateTime.now());
            wrapper.eq(WfApprovalTask::getNodeType, 1);
            List<WfApprovalTask> timeoutTasks = approvalTaskService.list(wrapper);

            for (WfApprovalTask task : timeoutTasks) {
                try {
                    processTimeoutTask(task);
                } catch (Exception e) {
                    log.error("处理超时任务失败, taskId={}, error={}", task.getId(), e.getMessage(), e);
                }
            }

            log.info("超时任务处理完成, 共处理{}个任务", timeoutTasks.size());
        } catch (Exception e) {
            log.error("超时任务处理异常: {}", e.getMessage(), e);
        }
    }

    private void processTimeoutTask(WfApprovalTask task) {
        int remindCount = timeoutRemindService.countByTaskId(task.getId());

        WfTimeoutRemind remind = new WfTimeoutRemind();
        remind.setTaskId(task.getId());
        remind.setInstanceId(task.getInstanceId());
        remind.setAssigneeId(task.getAssigneeId());
        remind.setRemindType(remindCount == 0 ? 1 : 2);
        remind.setRemindCount(remindCount + 1);
        remind.setEscalateLevel(task.getEscalateLevel() != null ? task.getEscalateLevel() : 0);
        remind.setRemindTime(LocalDateTime.now());

        sendTimeoutRemindNotify(task, remind);

        WfNodeConfig nodeConfig = getNodeConfig(task);
        if (nodeConfig != null && nodeConfig.getTimeoutEscalateLevels() != null
                && nodeConfig.getTimeoutEscalateLevels() > 0) {
            int currentLevel = task.getEscalateLevel() != null ? task.getEscalateLevel() : 0;
            if (currentLevel < nodeConfig.getTimeoutEscalateLevels()) {
                try {
                    Long escalateToUserId = escalateToLeader(task);
                    if (escalateToUserId != null) {
                        currentLevel++;
                        task.setEscalateLevel(currentLevel);
                        remind.setEscalateLevel(currentLevel);
                        remind.setEscalateToUserId(escalateToUserId);
                        approvalTaskService.updateById(task);
                        log.info("超时任务升级, taskId={}, escalateLevel={}, escalateToUserId={}",
                                task.getId(), currentLevel, escalateToUserId);
                    }
                } catch (Exception e) {
                    log.error("超时任务升级失败, taskId={}, error={}", task.getId(), e.getMessage(), e);
                }
            }
        }

        timeoutRemindService.save(remind);

        log.info("超时催办, taskId={}, assigneeId={}, remindCount={}, escalateLevel={}",
                task.getId(), task.getAssigneeId(), remindCount + 1, remind.getEscalateLevel());
    }

    private WfNodeConfig getNodeConfig(WfApprovalTask task) {
        try {
            WfProcessInstance instance = processInstanceService.getById(task.getInstanceId());
            if (instance == null) {
                return null;
            }
            return nodeConfigService.getByNodeId(instance.getProcessVersionId(), task.getNodeId());
        } catch (Exception e) {
            log.warn("获取节点配置失败, taskId={}, error={}", task.getId(), e.getMessage());
            return null;
        }
    }

    private Long escalateToLeader(WfApprovalTask task) {
        try {
            SysUser currentAssignee = null;
            try {
                SysUserService userService = SpringContextHolder.getBean(SysUserService.class);
                if (userService != null && task.getAssigneeId() != null) {
                    currentAssignee = userService.getById(task.getAssigneeId());
                }
            } catch (Exception e) {
                log.warn("获取当前审批人信息失败, taskId={}, error={}", task.getId(), e.getMessage());
            }

            Long leaderUserId = null;
            if (currentAssignee != null && currentAssignee.getDeptId() != null) {
                leaderUserId = findDeptLeader(currentAssignee.getDeptId());
            }

            if (leaderUserId == null) {
                log.warn("未找到上级主管, taskId={}, assigneeId={}", task.getId(), task.getAssigneeId());
                return null;
            }

            if (StrUtil.isBlank(task.getFlowableTaskId())) {
                log.warn("Flowable任务ID为空，无法创建加签任务, taskId={}", task.getId());
                return leaderUserId;
            }

            Task flowableTask = taskService.createTaskQuery()
                    .taskId(task.getFlowableTaskId())
                    .singleResult();
            if (flowableTask == null) {
                log.warn("Flowable任务不存在，无法创建加签任务, flowableTaskId={}", task.getFlowableTaskId());
                return leaderUserId;
            }

            Task newTask = taskService.newTask();
            newTask.setName(task.getNodeName() + "(超时升级加签)");
            newTask.setTaskDefinitionKey(task.getNodeId() + "_escalate");
            newTask.setProcessInstanceId(flowableTask.getProcessInstanceId());
            newTask.setExecutionId(flowableTask.getExecutionId());
            newTask.setAssignee(leaderUserId.toString());
            newTask.setPriority(flowableTask.getPriority() + 10);
            taskService.saveTask(newTask);

            WfApprovalTask newApprovalTask = new WfApprovalTask();
            newApprovalTask.setTaskNo(generateTaskNo());
            newApprovalTask.setInstanceId(task.getInstanceId());
            newApprovalTask.setFlowableTaskId(newTask.getId());
            newApprovalTask.setFlowableExecutionId(flowableTask.getExecutionId());
            newApprovalTask.setProcessKey(task.getProcessKey());
            newApprovalTask.setNodeId(task.getNodeId());
            newApprovalTask.setNodeName(task.getNodeName() + "(超时升级加签)");
            newApprovalTask.setNodeType(1);
            newApprovalTask.setAssigneeId(leaderUserId);
            newApprovalTask.setAssignTime(LocalDateTime.now());
            newApprovalTask.setTaskStatus(TaskStatusEnum.PENDING.getCode());
            newApprovalTask.setSourceType(4);
            newApprovalTask.setSourceTaskId(task.getId());
            newApprovalTask.setEscalateLevel(task.getEscalateLevel());
            approvalTaskService.save(newApprovalTask);

            sendEscalateNotify(task, leaderUserId);

            return leaderUserId;
        } catch (Exception e) {
            log.error("升级给上级主管失败, taskId={}, error={}", task.getId(), e.getMessage(), e);
            return null;
        }
    }

    private Long findDeptLeader(Long deptId) {
        try {
            com.innerworkflow.auth.service.SysDeptService deptService =
                    SpringContextHolder.getBean(com.innerworkflow.auth.service.SysDeptService.class);
            if (deptService != null) {
                com.innerworkflow.auth.entity.SysDepartment dept = deptService.getById(deptId);
                if (dept != null && dept.getLeaderUserId() != null) {
                    return dept.getLeaderUserId();
                }
                if (dept != null && dept.getParentId() != null) {
                    com.innerworkflow.auth.entity.SysDepartment parentDept =
                            deptService.getById(dept.getParentId());
                    if (parentDept != null && parentDept.getLeaderUserId() != null) {
                        return parentDept.getLeaderUserId();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("查询部门主管失败, deptId={}, error={}", deptId, e.getMessage());
        }
        return null;
    }

    private void sendTimeoutRemindNotify(WfApprovalTask task, WfTimeoutRemind remind) {
        try {
            WfProcessInstance instance = processInstanceService.getById(task.getInstanceId());
            if (instance == null || task.getAssigneeId() == null) {
                return;
            }

            NotifySendDTO sendDTO = new NotifySendDTO();
            sendDTO.setEventType(EventTypeEnum.TIMEOUT_REMIND.getCode());
            sendDTO.setBusinessType("WORKFLOW");
            sendDTO.setInstanceId(instance.getId());
            sendDTO.setTaskId(task.getId());
            sendDTO.setReceiverUserId(task.getAssigneeId());

            Map<String, Object> params = new HashMap<>();
            params.put("processTitle", instance.getTitle());
            params.put("instanceNo", instance.getInstanceNo());
            params.put("processKey", instance.getProcessKey());
            params.put("nodeName", task.getNodeName());
            params.put("dueTime", task.getDueTime());
            params.put("remindCount", remind.getRemindCount());
            params.put("escalateLevel", remind.getEscalateLevel());
            params.put("taskId", task.getId());
            params.put("instanceId", instance.getId());
            sendDTO.setParams(params);

            notifyService.sendNotify(sendDTO);
            log.info("超时提醒通知已发送, taskId={}, assigneeId={}, remindCount={}",
                    task.getId(), task.getAssigneeId(), remind.getRemindCount());
        } catch (Exception e) {
            log.error("发送超时提醒通知失败, taskId={}, error={}", task.getId(), e.getMessage(), e);
        }
    }

    private void sendEscalateNotify(WfApprovalTask task, Long escalateToUserId) {
        try {
            WfProcessInstance instance = processInstanceService.getById(task.getInstanceId());
            if (instance == null) {
                return;
            }

            NotifySendDTO sendDTO = new NotifySendDTO();
            sendDTO.setEventType(EventTypeEnum.TIMEOUT_REMIND.getCode());
            sendDTO.setBusinessType("WORKFLOW");
            sendDTO.setInstanceId(instance.getId());
            sendDTO.setTaskId(task.getId());
            sendDTO.setReceiverUserId(escalateToUserId);

            Map<String, Object> params = new HashMap<>();
            params.put("processTitle", instance.getTitle());
            params.put("instanceNo", instance.getInstanceNo());
            params.put("processKey", instance.getProcessKey());
            params.put("nodeName", task.getNodeName());
            params.put("dueTime", task.getDueTime());
            params.put("remindCount", "升级通知");
            params.put("escalateLevel", task.getEscalateLevel());
            params.put("escalateFromUserId", task.getAssigneeId());
            params.put("taskId", task.getId());
            params.put("instanceId", instance.getId());
            sendDTO.setParams(params);

            notifyService.sendNotify(sendDTO);
            log.info("超时升级通知已发送, taskId={}, escalateToUserId={}", task.getId(), escalateToUserId);
        } catch (Exception e) {
            log.error("发送超时升级通知失败, taskId={}, error={}", task.getId(), e.getMessage(), e);
        }
    }

    @Scheduled(cron = "0 0 9 * * ?")
    public void processSoonTimeoutTasks() {
        log.info("开始处理即将超时任务提醒...");

        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime soonTimeoutTime = now.plusHours(24);

            LambdaQueryWrapper<WfApprovalTask> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(WfApprovalTask::getTaskStatus, TaskStatusEnum.PENDING.getCode());
            wrapper.isNotNull(WfApprovalTask::getDueTime);
            wrapper.gt(WfApprovalTask::getDueTime, now);
            wrapper.lt(WfApprovalTask::getDueTime, soonTimeoutTime);
            wrapper.eq(WfApprovalTask::getNodeType, 1);
            List<WfApprovalTask> soonTimeoutTasks = approvalTaskService.list(wrapper);

            for (WfApprovalTask task : soonTimeoutTasks) {
                try {
                    sendSoonTimeoutRemind(task);
                } catch (Exception e) {
                    log.error("发送即将超时提醒失败, taskId={}, error={}", task.getId(), e.getMessage(), e);
                }
            }

            log.info("即将超时任务提醒完成, 共处理{}个任务", soonTimeoutTasks.size());
        } catch (Exception e) {
            log.error("即将超时任务处理异常: {}", e.getMessage(), e);
        }
    }

    private void sendSoonTimeoutRemind(WfApprovalTask task) {
        WfTimeoutRemind remind = new WfTimeoutRemind();
        remind.setTaskId(task.getId());
        remind.setInstanceId(task.getInstanceId());
        remind.setAssigneeId(task.getAssigneeId());
        remind.setRemindType(1);
        remind.setRemindCount(1);
        remind.setEscalateLevel(0);
        remind.setRemindTime(LocalDateTime.now());

        sendTimeoutRemindNotify(task, remind);

        timeoutRemindService.save(remind);

        log.info("即将超时提醒, taskId={}, assigneeId={}, dueTime={}",
                task.getId(), task.getAssigneeId(), task.getDueTime());
    }

    @Scheduled(cron = "0 */10 * * * ?")
    public void retryFailedReminds() {
        log.info("开始重试超时提醒记录...");
        try {
            LambdaQueryWrapper<WfTimeoutRemind> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(WfTimeoutRemind::getRemindType, 0);
            wrapper.last("LIMIT 100");
            List<WfTimeoutRemind> pendingReminds = timeoutRemindService.list(wrapper);

            for (WfTimeoutRemind remind : pendingReminds) {
                try {
                    WfApprovalTask task = approvalTaskService.getById(remind.getTaskId());
                    if (task != null) {
                        sendTimeoutRemindNotify(task, remind);
                    }
                } catch (Exception e) {
                    log.error("重试超时提醒失败, remindId={}, error={}", remind.getId(), e.getMessage(), e);
                }
            }

            log.info("超时提醒记录重试完成, 共处理{}条", pendingReminds.size());
        } catch (Exception e) {
            log.error("重试超时提醒异常: {}", e.getMessage(), e);
        }
    }

    private String generateTaskNo() {
        return "TK" + System.currentTimeMillis() + cn.hutool.core.util.IdUtil.randomUUID().substring(0, 4).toUpperCase();
    }
}
