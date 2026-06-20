package com.innerworkflow.approval.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.innerworkflow.approval.entity.WfApprovalTask;
import com.innerworkflow.approval.entity.WfProcessInstance;
import com.innerworkflow.approval.entity.WfTimeoutRemind;
import com.innerworkflow.approval.mapper.WfTimeoutRemindMapper;
import com.innerworkflow.approval.service.WfApprovalTaskService;
import com.innerworkflow.approval.service.WfProcessInstanceService;
import com.innerworkflow.approval.service.WfTimeoutRemindService;
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

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WfTimeoutRemindServiceImpl extends ServiceImpl<WfTimeoutRemindMapper, WfTimeoutRemind> implements WfTimeoutRemindService {

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
}
