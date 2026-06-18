package com.innerworkflow.approval.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.innerworkflow.approval.dto.WfDoneTaskQueryDTO;
import com.innerworkflow.approval.dto.WfTodoTaskQueryDTO;
import com.innerworkflow.approval.entity.WfApprovalTask;
import com.innerworkflow.approval.entity.WfProcessInstance;
import com.innerworkflow.approval.mapper.WfApprovalTaskMapper;
import com.innerworkflow.approval.service.WfApprovalTaskService;
import com.innerworkflow.approval.service.WfProcessInstanceService;
import com.innerworkflow.approval.vo.WfApprovalTaskVO;
import com.innerworkflow.common.dto.ApprovalAiFeaturesDTO;
import com.innerworkflow.common.service.AiRecommendationService;
import com.innerworkflow.common.vo.AiRecommendationVO;
import com.innerworkflow.common.enums.TaskStatusEnum;
import com.innerworkflow.common.util.SecurityUtils;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WfApprovalTaskServiceImpl extends ServiceImpl<WfApprovalTaskMapper, WfApprovalTask> implements WfApprovalTaskService {

    private final WfProcessInstanceService processInstanceService;
    private final AiRecommendationService aiRecommendationService;

    @Override
    public IPage<WfApprovalTaskVO> pageTodo(WfTodoTaskQueryDTO queryDTO) {
        Long userId = SecurityUtils.getCurrentUserId();
        LambdaQueryWrapper<WfApprovalTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfApprovalTask::getAssigneeId, userId);
        wrapper.eq(WfApprovalTask::getTaskStatus, TaskStatusEnum.PENDING.getCode());
        wrapper.eq(WfApprovalTask::getNodeType, 1);
        if (StrUtil.isNotBlank(queryDTO.getProcessKey())) {
            wrapper.eq(WfApprovalTask::getProcessKey, queryDTO.getProcessKey());
        }
        if (queryDTO.getPriority() != null) {
            wrapper.eq(WfApprovalTask::getAction, queryDTO.getPriority());
        }
        wrapper.orderByDesc(WfApprovalTask::getCreateTime);
        return this.page(queryDTO.buildPage("create_time desc"), wrapper)
                .convert(this::convertToVO);
    }

    @Override
    public IPage<WfApprovalTaskVO> pageDone(WfDoneTaskQueryDTO queryDTO) {
        Long userId = SecurityUtils.getCurrentUserId();
        LambdaQueryWrapper<WfApprovalTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfApprovalTask::getAssigneeId, userId);
        wrapper.in(WfApprovalTask::getTaskStatus,
                TaskStatusEnum.DONE.getCode(),
                TaskStatusEnum.TRANSFERRED.getCode(),
                TaskStatusEnum.DELEGATED.getCode());
        wrapper.eq(WfApprovalTask::getNodeType, 1);
        if (StrUtil.isNotBlank(queryDTO.getProcessKey())) {
            wrapper.eq(WfApprovalTask::getProcessKey, queryDTO.getProcessKey());
        }
        if (queryDTO.getAction() != null) {
            wrapper.eq(WfApprovalTask::getAction, queryDTO.getAction());
        }
        wrapper.orderByDesc(WfApprovalTask::getActionTime);
        return this.page(queryDTO.buildPage("action_time desc"), wrapper)
                .convert(this::convertToVO);
    }

    @Override
    public WfApprovalTask getByFlowableTaskId(String flowableTaskId) {
        LambdaQueryWrapper<WfApprovalTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfApprovalTask::getFlowableTaskId, flowableTaskId);
        return this.getOne(wrapper);
    }

    @Override
    public List<WfApprovalTask> listByInstanceId(Long instanceId) {
        LambdaQueryWrapper<WfApprovalTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfApprovalTask::getInstanceId, instanceId);
        wrapper.orderByDesc(WfApprovalTask::getCreateTime);
        return this.list(wrapper);
    }

    @Override
    public List<WfApprovalTask> listTodoByInstanceId(Long instanceId) {
        LambdaQueryWrapper<WfApprovalTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfApprovalTask::getInstanceId, instanceId);
        wrapper.eq(WfApprovalTask::getTaskStatus, TaskStatusEnum.PENDING.getCode());
        return this.list(wrapper);
    }

    @Override
    public boolean updateByFlowableTaskId(String flowableTaskId, WfApprovalTask task) {
        LambdaUpdateWrapper<WfApprovalTask> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(WfApprovalTask::getFlowableTaskId, flowableTaskId);
        return this.update(task, wrapper);
    }

    @Override
    public long countTodoByUserId(Long userId) {
        LambdaQueryWrapper<WfApprovalTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfApprovalTask::getAssigneeId, userId);
        wrapper.eq(WfApprovalTask::getTaskStatus, TaskStatusEnum.PENDING.getCode());
        wrapper.eq(WfApprovalTask::getNodeType, 1);
        return this.count(wrapper);
    }

    private WfApprovalTaskVO convertToVO(WfApprovalTask task) {
        WfApprovalTaskVO vo = new WfApprovalTaskVO();
        BeanUtils.copyProperties(task, vo);
        TaskStatusEnum statusEnum = TaskStatusEnum.getByCode(task.getTaskStatus());
        if (statusEnum != null) {
            vo.setTaskStatusName(statusEnum.getDesc());
        }
        WfProcessInstance instance = processInstanceService.getById(task.getInstanceId());
        if (instance != null) {
            vo.setTitle(instance.getTitle());
            vo.setStartUserId(instance.getStartUserId());
            vo.setInstanceNo(instance.getInstanceNo());
        }

        if (TaskStatusEnum.PENDING.getCode().equals(task.getTaskStatus())) {
            try {
                AiRecommendationVO rec = aiRecommendationService.getRecommendation(task.getId());
                if (rec != null) {
                    vo.setAiRecommendation(rec);
                    vo.setAiRecommendationId(rec.getId());
                }
            } catch (Exception e) {
                log.debug("AI推荐获取失败, taskId={}, error={}", task.getId(), e.getMessage());
            }
        }

        return vo;
    }
}
