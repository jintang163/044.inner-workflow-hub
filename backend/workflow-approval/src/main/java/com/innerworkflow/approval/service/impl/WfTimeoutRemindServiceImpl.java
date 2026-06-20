package com.innerworkflow.approval.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.innerworkflow.approval.dto.WfBatchRemindDTO;
import com.innerworkflow.approval.entity.WfApprovalTask;
import com.innerworkflow.approval.entity.WfProcessInstance;
import com.innerworkflow.approval.entity.WfTimeoutRemind;
import com.innerworkflow.approval.mapper.WfTimeoutRemindMapper;
import com.innerworkflow.approval.service.WfApprovalTaskService;
import com.innerworkflow.approval.service.WfProcessInstanceService;
import com.innerworkflow.approval.service.WfTimeoutRemindService;
import com.innerworkflow.approval.vo.WfBatchRemindResultVO;
import com.innerworkflow.common.context.TenantContext;
import com.innerworkflow.common.dto.PageQuery;
import com.innerworkflow.common.enums.TaskStatusEnum;
import com.innerworkflow.common.exception.BusinessException;
import com.innerworkflow.common.util.SecurityUtils;
import com.innerworkflow.notify.dto.NotifySendDTO;
import com.innerworkflow.notify.enums.EventTypeEnum;
import com.innerworkflow.notify.service.WfNotifyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WfTimeoutRemindServiceImpl extends ServiceImpl<WfTimeoutRemindMapper, WfTimeoutRemind> implements WfTimeoutRemindService {

    private static final int DEFAULT_REMIND_INTERVAL_MINUTES = 5;

    private static final int MAX_REMIND_PER_HOUR = 10;

    private final WfApprovalTaskService approvalTaskService;
    private final WfProcessInstanceService processInstanceService;
    private final WfNotifyService notifyService;

    @Override
    public List<WfTimeoutRemind> listByTaskId(Long taskId) {
        LambdaQueryWrapper<WfTimeoutRemind> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfTimeoutRemind::getTaskId, taskId);
        wrapper.orderByDesc(WfTimeoutRemind::getRemindTime);
        return this.list(wrapper);
    }

    @Override
    public int countByTaskId(Long taskId) {
        LambdaQueryWrapper<WfTimeoutRemind> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfTimeoutRemind::getTaskId, taskId);
        return (int) this.count(wrapper);
    }

    @Override
    public List<WfTimeoutRemind> listByInstanceId(Long instanceId) {
        LambdaQueryWrapper<WfTimeoutRemind> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfTimeoutRemind::getInstanceId, instanceId);
        wrapper.orderByDesc(WfTimeoutRemind::getRemindTime);
        return this.list(wrapper);
    }

    @Override
    public boolean manualRemind(Long taskId, String remark) {
        WfApprovalTask task = approvalTaskService.getById(taskId);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }
        if (!TaskStatusEnum.PENDING.getCode().equals(task.getTaskStatus())) {
            throw new BusinessException("只有待办任务才能催办");
        }

        Long currentUserId = SecurityUtils.getCurrentUserId();
        WfProcessInstance instance = processInstanceService.getById(task.getInstanceId());

        boolean canRemind = false;
        if (instance != null && currentUserId.equals(instance.getStartUserId())) {
            canRemind = true;
        }
        if (currentUserId.equals(task.getAssigneeId())) {
            canRemind = true;
        }

        if (!canRemind) {
            throw new BusinessException("只有发起人或当前审批人才能催办");
        }

        int manualCount = countManualRemindByTaskId(taskId);

        WfTimeoutRemind remind = new WfTimeoutRemind();
        remind.setTaskId(task.getId());
        remind.setInstanceId(task.getInstanceId());
        remind.setAssigneeId(task.getAssigneeId());
        remind.setRemindType(2);
        remind.setRemindSource(2);
        remind.setRemindBy(currentUserId);
        remind.setRemark(remark);
        remind.setRemindCount(manualCount + 1);
        remind.setEscalateLevel(task.getEscalateLevel() != null ? task.getEscalateLevel() : 0);
        remind.setRemindTime(LocalDateTime.now());
        remind.setTenantId(TenantContext.getTenantId());
        remind.setCreateBy(currentUserId);
        remind.setCreateTime(LocalDateTime.now());

        sendManualRemindNotify(task, instance, remind);

        return this.save(remind);
    }

    @Override
    public IPage<WfTimeoutRemind> pageByTaskId(Long taskId, PageQuery query) {
        LambdaQueryWrapper<WfTimeoutRemind> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfTimeoutRemind::getTaskId, taskId);
        wrapper.orderByDesc(WfTimeoutRemind::getRemindTime);
        Page<WfTimeoutRemind> page = new Page<>(query.getPageNum(), query.getPageSize());
        return this.page(page, wrapper);
    }

    @Override
    public IPage<WfTimeoutRemind> pageByInstanceId(Long instanceId, PageQuery query) {
        LambdaQueryWrapper<WfTimeoutRemind> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfTimeoutRemind::getInstanceId, instanceId);
        wrapper.orderByDesc(WfTimeoutRemind::getRemindTime);
        Page<WfTimeoutRemind> page = new Page<>(query.getPageNum(), query.getPageSize());
        return this.page(page, wrapper);
    }

    @Override
    public int countManualRemindByTaskId(Long taskId) {
        LambdaQueryWrapper<WfTimeoutRemind> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfTimeoutRemind::getTaskId, taskId);
        wrapper.eq(WfTimeoutRemind::getRemindSource, 2);
        return (int) this.count(wrapper);
    }

    private void sendManualRemindNotify(WfApprovalTask task, WfProcessInstance instance, WfTimeoutRemind remind) {
        try {
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
            params.put("remindType", "手动催办");
            params.put("remindCount", remind.getRemindCount());
            params.put("remindBy", remind.getRemindBy());
            params.put("remark", remind.getRemark());
            params.put("taskId", task.getId());
            params.put("instanceId", instance.getId());
            sendDTO.setParams(params);

            notifyService.sendNotify(sendDTO);
        } catch (Exception e) {
            log.error("发送手动催办通知失败, taskId={}, error={}", task.getId(), e.getMessage(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WfBatchRemindResultVO batchRemind(WfBatchRemindDTO dto) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        WfBatchRemindResultVO result = new WfBatchRemindResultVO();
        List<Long> taskIds = dto.getTaskIds();
        result.setTotalCount(taskIds.size());

        if (SecurityUtils.isSuperAdmin()) {
            checkRateLimit(currentUserId);
        }

        List<WfApprovalTask> tasks = approvalTaskService.listByIds(taskIds);
        Map<Long, WfApprovalTask> taskMap = tasks.stream()
                .collect(Collectors.toMap(WfApprovalTask::getId, t -> t));

        Set<Long> instanceIds = tasks.stream()
                .map(WfApprovalTask::getInstanceId)
                .collect(Collectors.toSet());
        List<WfProcessInstance> instances = processInstanceService.listByIds(instanceIds);
        Map<Long, WfProcessInstance> instanceMap = instances.stream()
                .collect(Collectors.toMap(WfProcessInstance::getId, i -> i));

        List<WfTimeoutRemind> remindRecords = new ArrayList<>();
        Map<Long, List<RemindTaskInfo>> assigneeTaskMap = new HashMap<>();
        int successCount = 0;

        for (Long taskId : taskIds) {
            WfApprovalTask task = taskMap.get(taskId);
            if (task == null) {
                result.getFailItems().add(new WfBatchRemindResultVO.RemindFailItem(taskId, null, "任务不存在"));
                continue;
            }

            if (!TaskStatusEnum.PENDING.getCode().equals(task.getTaskStatus())) {
                result.getFailItems().add(new WfBatchRemindResultVO.RemindFailItem(taskId, task.getTaskNo(), "只有待办任务才能催办"));
                continue;
            }

            WfProcessInstance instance = instanceMap.get(task.getInstanceId());

            boolean canRemind = false;
            if (instance != null && currentUserId.equals(instance.getStartUserId())) {
                canRemind = true;
            }
            if (SecurityUtils.isSuperAdmin()) {
                canRemind = true;
            }

            if (!canRemind) {
                result.getFailItems().add(new WfBatchRemindResultVO.RemindFailItem(taskId, task.getTaskNo(), "只有发起人或管理员才能催办"));
                continue;
            }

            if (!checkRemindInterval(taskId, DEFAULT_REMIND_INTERVAL_MINUTES)) {
                result.getFailItems().add(new WfBatchRemindResultVO.RemindFailItem(taskId, task.getTaskNo(), "催办间隔不足，请稍后再试"));
                continue;
            }

            int manualCount = countManualRemindByTaskId(taskId);

            WfTimeoutRemind remind = new WfTimeoutRemind();
            remind.setTaskId(task.getId());
            remind.setInstanceId(task.getInstanceId());
            remind.setAssigneeId(task.getAssigneeId());
            remind.setRemindType(2);
            remind.setRemindSource(2);
            remind.setRemindBy(currentUserId);
            remind.setRemark(dto.getRemark());
            remind.setRemindCount(manualCount + 1);
            remind.setEscalateLevel(task.getEscalateLevel() != null ? task.getEscalateLevel() : 0);
            remind.setRemindTime(LocalDateTime.now());
            remind.setTenantId(TenantContext.getTenantId());
            remind.setCreateBy(currentUserId);
            remind.setCreateTime(LocalDateTime.now());

            remindRecords.add(remind);

            if (task.getAssigneeId() != null) {
                assigneeTaskMap.computeIfAbsent(task.getAssigneeId(), k -> new ArrayList<>())
                        .add(new RemindTaskInfo(task, instance, remind));
            }

            successCount++;
        }

        if (!remindRecords.isEmpty()) {
            this.saveBatch(remindRecords);
        }

        int messageCount = 0;
        for (Map.Entry<Long, List<RemindTaskInfo>> entry : assigneeTaskMap.entrySet()) {
            Long assigneeId = entry.getKey();
            List<RemindTaskInfo> taskInfos = entry.getValue();

            if (taskInfos.size() == 1) {
                RemindTaskInfo info = taskInfos.get(0);
                sendManualRemindNotify(info.task, info.instance, info.remind);
            } else {
                sendAggregatedRemindNotify(assigneeId, taskInfos, dto.getRemark());
            }
            messageCount++;
        }

        result.setSuccessCount(successCount);
        result.setFailCount(result.getFailItems().size());
        result.setRemindMessageCount(messageCount);

        log.info("批量催办完成, total={}, success={}, fail={}, messages={}",
                result.getTotalCount(), successCount, result.getFailCount(), messageCount);

        return result;
    }

    @Override
    public LocalDateTime getLastRemindTime(Long taskId) {
        LambdaQueryWrapper<WfTimeoutRemind> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfTimeoutRemind::getTaskId, taskId);
        wrapper.eq(WfTimeoutRemind::getRemindSource, 2);
        wrapper.orderByDesc(WfTimeoutRemind::getRemindTime);
        wrapper.last("LIMIT 1");
        WfTimeoutRemind remind = this.getOne(wrapper);
        return remind != null ? remind.getRemindTime() : null;
    }

    @Override
    public boolean checkRemindInterval(Long taskId, int intervalMinutes) {
        LocalDateTime lastRemindTime = getLastRemindTime(taskId);
        if (lastRemindTime == null) {
            return true;
        }
        long minutes = Duration.between(lastRemindTime, LocalDateTime.now()).toMinutes();
        return minutes >= intervalMinutes;
    }

    private void checkRateLimit(Long userId) {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        LambdaQueryWrapper<WfTimeoutRemind> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfTimeoutRemind::getRemindBy, userId);
        wrapper.eq(WfTimeoutRemind::getRemindSource, 2);
        wrapper.ge(WfTimeoutRemind::getRemindTime, oneHourAgo);
        long count = this.count(wrapper);
        if (count >= MAX_REMIND_PER_HOUR) {
            throw new BusinessException("催办过于频繁，请稍后再试");
        }
    }

    private void sendAggregatedRemindNotify(Long assigneeId, List<RemindTaskInfo> taskInfos, String remark) {
        try {
            if (taskInfos.isEmpty()) {
                return;
            }

            RemindTaskInfo firstInfo = taskInfos.get(0);
            WfProcessInstance firstInstance = firstInfo.instance;
            if (firstInstance == null) {
                return;
            }

            NotifySendDTO sendDTO = new NotifySendDTO();
            sendDTO.setEventType(EventTypeEnum.TIMEOUT_REMIND.getCode());
            sendDTO.setBusinessType("WORKFLOW");
            sendDTO.setInstanceId(firstInstance.getId());
            sendDTO.setTaskId(firstInfo.task.getId());
            sendDTO.setReceiverUserId(assigneeId);

            List<String> taskSummaries = taskInfos.stream()
                    .map(info -> {
                        WfProcessInstance inst = info.instance;
                        return inst != null ? String.format("- %s（%s）", inst.getTitle(), info.task.getNodeName()) : "";
                    })
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());

            String aggregatedContent = String.format("您有 %d 条待办任务被催办：\n%s",
                    taskInfos.size(), String.join("\n", taskSummaries));

            Map<String, Object> params = new HashMap<>();
            params.put("processTitle", "批量催办通知");
            params.put("instanceNo", "BATCH_REMIND");
            params.put("processKey", "BATCH_REMIND");
            params.put("nodeName", "批量处理");
            params.put("remindType", "批量催办");
            params.put("remindCount", taskInfos.size());
            params.put("remindBy", firstInfo.remind.getRemindBy());
            params.put("remark", remark != null ? remark : aggregatedContent);
            params.put("aggregated", true);
            params.put("taskCount", taskInfos.size());
            params.put("taskSummaries", taskSummaries);
            sendDTO.setParams(params);

            notifyService.sendNotify(sendDTO);
        } catch (Exception e) {
            log.error("发送批量催办通知失败, assigneeId={}, error={}", assigneeId, e.getMessage(), e);
        }
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    private static class RemindTaskInfo {
        private WfApprovalTask task;
        private WfProcessInstance instance;
        private WfTimeoutRemind remind;
    }
}
