package com.innerworkflow.approval.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.innerworkflow.approval.entity.WfApprovalTask;
import com.innerworkflow.approval.entity.WfProcessInstance;
import com.innerworkflow.approval.entity.WfTimeoutRemind;
import com.innerworkflow.approval.service.WfApprovalTaskService;
import com.innerworkflow.approval.service.WfProcessInstanceService;
import com.innerworkflow.approval.service.WfTimeoutRemindService;
import com.innerworkflow.common.enums.TaskStatusEnum;
import com.innerworkflow.notify.dto.NotifySendDTO;
import com.innerworkflow.notify.enums.EventTypeEnum;
import com.innerworkflow.notify.service.WfNotifyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
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
    private final WfNotifyService notifyService;

    @Scheduled(cron = "0 */5 * * * ?")
    public void processTimeoutTasks() {
        log.info("开始处理超时任务提醒...");

        try {
            LambdaQueryWrapper<WfApprovalTask> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(WfApprovalTask::getTaskStatus, TaskStatusEnum.PENDING.getCode());
            wrapper.isNotNull(WfApprovalTask::getDueTime);
            wrapper.lt(WfApprovalTask::getDueTime, LocalDateTime.now());
            wrapper.eq(WfApprovalTask::getNodeType, 1);
            List<WfApprovalTask> timeoutTasks = approvalTaskService.list(wrapper);

            for (WfApprovalTask task : timeoutTasks) {
                try {
                    processTimeoutRemind(task);
                } catch (Exception e) {
                    log.error("处理超时任务提醒失败, taskId={}, error={}", task.getId(), e.getMessage(), e);
                }
            }

            log.info("超时任务提醒处理完成, 共处理{}个任务", timeoutTasks.size());
        } catch (Exception e) {
            log.error("超时任务提醒处理异常: {}", e.getMessage(), e);
        }
    }

    private void processTimeoutRemind(WfApprovalTask task) {
        int remindCount = timeoutRemindService.countByTaskId(task.getId());

        WfTimeoutRemind remind = new WfTimeoutRemind();
        remind.setTaskId(task.getId());
        remind.setInstanceId(task.getInstanceId());
        remind.setAssigneeId(task.getAssigneeId());
        remind.setRemindType(remindCount == 0 ? 1 : 2);
        remind.setRemindSource(1);
        remind.setRemindCount(remindCount + 1);
        remind.setEscalateLevel(task.getEscalateLevel() != null ? task.getEscalateLevel() : 0);
        remind.setRemindTime(LocalDateTime.now());
        remind.setCreateTime(LocalDateTime.now());

        sendTimeoutRemindNotify(task, remind);

        timeoutRemindService.save(remind);

        log.info("超时催办, taskId={}, assigneeId={}, remindCount={}, escalateLevel={}",
                task.getId(), task.getAssigneeId(), remindCount + 1, remind.getEscalateLevel());
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
        remind.setRemindSource(1);
        remind.setRemindCount(1);
        remind.setEscalateLevel(0);
        remind.setRemindTime(LocalDateTime.now());
        remind.setCreateTime(LocalDateTime.now());

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
}
