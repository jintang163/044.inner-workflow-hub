package com.innerworkflow.approval.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.innerworkflow.approval.entity.WfApprovalTask;
import com.innerworkflow.approval.entity.WfEscalationHistory;
import com.innerworkflow.approval.entity.WfEscalationRule;
import com.innerworkflow.approval.entity.WfProcessInstance;
import com.innerworkflow.approval.service.*;
import com.innerworkflow.auth.entity.SysUser;
import com.innerworkflow.auth.service.SysUserService;
import com.innerworkflow.common.enums.TaskStatusEnum;
import com.innerworkflow.common.util.SpringContextHolder;
import com.innerworkflow.notify.dto.NotifySendDTO;
import com.innerworkflow.notify.enums.EventTypeEnum;
import com.innerworkflow.notify.service.WfNotifyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WfEscalationServiceImpl implements WfEscalationService {

    private final WfApprovalTaskService approvalTaskService;
    private final WfEscalationRuleService escalationRuleService;
    private final WfEscalationHistoryService escalationHistoryService;
    private final WfProcessInstanceService processInstanceService;
    private final WfNotifyService notifyService;
    private final TaskService taskService;

    @Override
    public void processEscalation() {
        log.info("开始处理超时升级任务...");

        try {
            LambdaQueryWrapper<WfApprovalTask> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(WfApprovalTask::getTaskStatus, TaskStatusEnum.PENDING.getCode());
            wrapper.isNotNull(WfApprovalTask::getDueTime);
            wrapper.lt(WfApprovalTask::getDueTime, LocalDateTime.now());
            wrapper.eq(WfApprovalTask::getNodeType, 1);
            List<WfApprovalTask> timeoutTasks = approvalTaskService.list(wrapper);

            log.info("发现{}个超时任务", timeoutTasks.size());

            for (WfApprovalTask task : timeoutTasks) {
                try {
                    WfProcessInstance instance = processInstanceService.getById(task.getInstanceId());
                    if (instance == null) {
                        continue;
                    }

                    List<WfEscalationRule> rules = escalationRuleService.listEnabledRules(
                            instance.getProcessKey(), task.getNodeId());

                    if (rules != null && !rules.isEmpty()) {
                        processTaskEscalation(task, rules);
                    }
                } catch (Exception e) {
                    log.error("处理任务升级失败, taskId={}, error={}", task.getId(), e.getMessage(), e);
                }
            }

            log.info("超时升级任务处理完成");
        } catch (Exception e) {
            log.error("超时升级任务处理异常: {}", e.getMessage(), e);
        }
    }

    @Override
    public void processTaskEscalation(WfApprovalTask task, List<WfEscalationRule> rules) {
        if (task.getDueTime() == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        long hoursOverdue = ChronoUnit.HOURS.between(task.getDueTime(), now);
        int currentLevel = task.getEscalateLevel() != null ? task.getEscalateLevel() : 0;

        for (WfEscalationRule rule : rules) {
            if (rule.getEscalateLevel() > currentLevel
                    && hoursOverdue >= rule.getTimeoutHours()) {
                boolean success = executeEscalation(task, rule);
                if (success) {
                    currentLevel = rule.getEscalateLevel();
                    task.setEscalateLevel(currentLevel);
                    approvalTaskService.updateById(task);
                }
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean executeEscalation(WfApprovalTask task, WfEscalationRule rule) {
        try {
            List<Long> toUserIds = resolveEscalateUsers(task, rule);
            if (toUserIds == null || toUserIds.isEmpty()) {
                log.warn("未找到升级目标用户, taskId={}, ruleId={}", task.getId(), rule.getId());
                return false;
            }

            int escalateAction = rule.getEscalateAction() != null ? rule.getEscalateAction() : 1;

            switch (escalateAction) {
                case 1:
                    doAddSignEscalation(task, rule, toUserIds);
                    break;
                case 2:
                    doTransferEscalation(task, rule, toUserIds);
                    break;
                case 3:
                    doNotifyOnlyEscalation(task, rule, toUserIds);
                    break;
                default:
                    doNotifyOnlyEscalation(task, rule, toUserIds);
                    break;
            }

            saveEscalationHistory(task, rule, toUserIds);

            log.info("任务升级执行成功, taskId={}, ruleId={}, level={}, action={}",
                    task.getId(), rule.getId(), rule.getEscalateLevel(), escalateAction);
            return true;
        } catch (Exception e) {
            log.error("任务升级执行失败, taskId={}, ruleId={}, error={}",
                    task.getId(), rule.getId(), e.getMessage(), e);
            return false;
        }
    }

    @Override
    public List<Long> resolveEscalateUsers(WfApprovalTask task, WfEscalationRule rule) {
        List<Long> userIds = new ArrayList<>();

        int escalateType = rule.getEscalateType() != null ? rule.getEscalateType() : 1;

        switch (escalateType) {
            case 1:
                Long leaderUserId = findDeptLeader(task.getAssigneeId());
                if (leaderUserId != null) {
                    userIds.add(leaderUserId);
                }
                break;
            case 2:
                userIds.addAll(findUsersByRole(rule.getEscalateTarget()));
                break;
            case 3:
                userIds.addAll(parseUserIds(rule.getEscalateTarget()));
                break;
            case 4:
                userIds.addAll(findAdminUsers());
                break;
            default:
                break;
        }

        return userIds;
    }

    private void doAddSignEscalation(WfApprovalTask task, WfEscalationRule rule, List<Long> userIds) {
        if (StrUtil.isBlank(task.getFlowableTaskId())) {
            log.warn("Flowable任务ID为空，无法创建加签任务, taskId={}", task.getId());
            doNotifyOnlyEscalation(task, rule, userIds);
            return;
        }

        Task flowableTask = taskService.createTaskQuery()
                .taskId(task.getFlowableTaskId())
                .singleResult();
        if (flowableTask == null) {
            log.warn("Flowable任务不存在，无法创建加签任务, flowableTaskId={}", task.getFlowableTaskId());
            doNotifyOnlyEscalation(task, rule, userIds);
            return;
        }

        for (Long userId : userIds) {
            try {
                Task newTask = taskService.newTask();
                newTask.setName(task.getNodeName() + "(超时升级加签)");
                newTask.setTaskDefinitionKey(task.getNodeId() + "_escalate_" + rule.getEscalateLevel());
                newTask.setProcessInstanceId(flowableTask.getProcessInstanceId());
                newTask.setExecutionId(flowableTask.getExecutionId());
                newTask.setAssignee(userId.toString());
                newTask.setPriority(flowableTask.getPriority() + 10 + rule.getEscalateLevel());
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
                newApprovalTask.setAssigneeId(userId);
                newApprovalTask.setAssignTime(LocalDateTime.now());
                newApprovalTask.setTaskStatus(TaskStatusEnum.PENDING.getCode());
                newApprovalTask.setSourceType(4);
                newApprovalTask.setSourceTaskId(task.getId());
                newApprovalTask.setEscalateLevel(rule.getEscalateLevel());
                approvalTaskService.save(newApprovalTask);

                sendEscalationNotify(task, rule, userId);
            } catch (Exception e) {
                log.error("创建升级加签任务失败, taskId={}, userId={}, error={}",
                        task.getId(), userId, e.getMessage(), e);
            }
        }
    }

    private void doTransferEscalation(WfApprovalTask task, WfEscalationRule rule, List<Long> userIds) {
        if (userIds.isEmpty()) {
            return;
        }

        Long firstUserId = userIds.get(0);
        try {
            if (StrUtil.isNotBlank(task.getFlowableTaskId())) {
                Task flowableTask = taskService.createTaskQuery()
                        .taskId(task.getFlowableTaskId())
                        .singleResult();
                if (flowableTask != null) {
                    taskService.setAssignee(task.getFlowableTaskId(), firstUserId.toString());
                }
            }

            task.setAssigneeId(firstUserId);
            task.setAssignTime(LocalDateTime.now());
            approvalTaskService.updateById(task);

            sendEscalationNotify(task, rule, firstUserId);
        } catch (Exception e) {
            log.error("转办升级失败, taskId={}, userId={}, error={}",
                    task.getId(), firstUserId, e.getMessage(), e);
        }
    }

    private void doNotifyOnlyEscalation(WfApprovalTask task, WfEscalationRule rule, List<Long> userIds) {
        for (Long userId : userIds) {
            try {
                sendEscalationNotify(task, rule, userId);
            } catch (Exception e) {
                log.error("发送升级通知失败, taskId={}, userId={}, error={}",
                        task.getId(), userId, e.getMessage(), e);
            }
        }
    }

    private void sendEscalationNotify(WfApprovalTask task, WfEscalationRule rule, Long userId) {
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
            sendDTO.setReceiverUserId(userId);

            Map<String, Object> params = new HashMap<>();
            params.put("processTitle", instance.getTitle());
            params.put("instanceNo", instance.getInstanceNo());
            params.put("processKey", instance.getProcessKey());
            params.put("nodeName", task.getNodeName());
            params.put("dueTime", task.getDueTime());
            params.put("escalateLevel", rule.getEscalateLevel());
            params.put("escalateType", rule.getEscalateType());
            params.put("escalateAction", rule.getEscalateAction());
            params.put("ruleName", rule.getRuleName());
            params.put("fromUserId", task.getAssigneeId());
            params.put("taskId", task.getId());
            params.put("instanceId", instance.getId());
            sendDTO.setParams(params);

            notifyService.sendNotify(sendDTO);
        } catch (Exception e) {
            log.error("发送升级通知失败, taskId={}, userId={}, error={}", task.getId(), userId, e.getMessage(), e);
        }
    }

    private void saveEscalationHistory(WfApprovalTask task, WfEscalationRule rule, List<Long> toUserIds) {
        WfEscalationHistory history = new WfEscalationHistory();
        history.setInstanceId(task.getInstanceId());
        history.setTaskId(task.getId());
        history.setRuleId(rule.getId());
        history.setEscalateLevel(rule.getEscalateLevel());
        history.setEscalateType(rule.getEscalateType());
        history.setEscalateTarget(rule.getEscalateTarget());
        history.setEscalateAction(rule.getEscalateAction());
        history.setFromUserId(task.getAssigneeId());
        history.setToUserIds(toUserIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
        history.setTriggerTime(LocalDateTime.now());
        history.setTriggerType(1);
        history.setRemark("超时自动升级");
        history.setTenantId(task.getTenantId());
        escalationHistoryService.save(history);
    }

    private Long findDeptLeader(Long userId) {
        try {
            SysUserService userService = SpringContextHolder.getBean(SysUserService.class);
            if (userService == null || userId == null) {
                return null;
            }

            SysUser user = userService.getById(userId);
            if (user == null || user.getDeptId() == null) {
                return null;
            }

            com.innerworkflow.auth.service.SysDeptService deptService =
                    SpringContextHolder.getBean(com.innerworkflow.auth.service.SysDeptService.class);
            if (deptService == null) {
                return null;
            }

            com.innerworkflow.auth.entity.SysDepartment dept = deptService.getById(user.getDeptId());
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
        } catch (Exception e) {
            log.warn("查询部门主管失败, userId={}, error={}", userId, e.getMessage());
        }
        return null;
    }

    private List<Long> findUsersByRole(String roleIds) {
        List<Long> userIds = new ArrayList<>();
        try {
            if (StrUtil.isBlank(roleIds)) {
                return userIds;
            }

            List<Long> roleIdList = parseUserIds(roleIds);
            if (roleIdList.isEmpty()) {
                return userIds;
            }

            SysUserService userService = SpringContextHolder.getBean(SysUserService.class);
            if (userService != null) {
                for (Long roleId : roleIdList) {
                    List<SysUser> users = userService.listByRoleId(roleId);
                    if (users != null) {
                        for (SysUser user : users) {
                            if (!userIds.contains(user.getId())) {
                                userIds.add(user.getId());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("查询角色用户失败, roleIds={}, error={}", roleIds, e.getMessage());
        }
        return userIds;
    }

    private List<Long> findAdminUsers() {
        List<Long> userIds = new ArrayList<>();
        try {
            SysUserService userService = SpringContextHolder.getBean(SysUserService.class);
            if (userService != null) {
                List<SysUser> adminUsers = userService.listByRoleCode("admin");
                if (adminUsers != null) {
                    for (SysUser user : adminUsers) {
                        userIds.add(user.getId());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("查询管理员用户失败, error={}", e.getMessage());
        }
        return userIds;
    }

    private List<Long> parseUserIds(String userIdsStr) {
        List<Long> userIds = new ArrayList<>();
        if (StrUtil.isBlank(userIdsStr)) {
            return userIds;
        }
        String[] parts = userIdsStr.split(",");
        for (String part : parts) {
            try {
                userIds.add(Long.parseLong(part.trim()));
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        return userIds;
    }

    private String generateTaskNo() {
        return "TK" + System.currentTimeMillis() + cn.hutool.core.util.IdUtil.randomUUID().substring(0, 4).toUpperCase();
    }
}
