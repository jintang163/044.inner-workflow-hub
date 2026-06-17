package com.innerworkflow.approval.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.innerworkflow.approval.entity.WfApprovalTask;
import com.innerworkflow.approval.entity.WfTimeoutRemind;
import com.innerworkflow.approval.service.WfApprovalTaskService;
import com.innerworkflow.approval.service.WfTimeoutRemindService;
import com.innerworkflow.common.enums.TaskStatusEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WfTimeoutTask {

    private final WfApprovalTaskService approvalTaskService;
    private final WfTimeoutRemindService timeoutRemindService;

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
        remind.setEscalateLevel(task.getEscalateLevel());
        remind.setRemindTime(LocalDateTime.now());
        timeoutRemindService.save(remind);

        log.info("超时催办, taskId={}, assigneeId={}, remindCount={}",
                task.getId(), task.getAssigneeId(), remindCount + 1);
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
        timeoutRemindService.save(remind);

        log.info("即将超时提醒, taskId={}, assigneeId={}, dueTime={}",
                task.getId(), task.getAssigneeId(), task.getDueTime());
    }
}
