package com.innerworkflow.approval.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.innerworkflow.approval.dto.WfDoneTaskQueryDTO;
import com.innerworkflow.approval.dto.WfTodoTaskQueryDTO;
import com.innerworkflow.approval.entity.WfApprovalHistory;
import com.innerworkflow.approval.entity.WfApprovalTask;
import com.innerworkflow.approval.entity.WfProcessInstance;
import com.innerworkflow.approval.mapper.WfApprovalTaskMapper;
import com.innerworkflow.approval.service.WfApprovalHistoryService;
import com.innerworkflow.approval.service.WfApprovalTaskService;
import com.innerworkflow.approval.service.WfProcessInstanceService;
import com.innerworkflow.approval.vo.WfApprovalTaskVO;
import com.innerworkflow.bpmn.entity.WfNodeConfig;
import com.innerworkflow.bpmn.service.WfNodeConfigService;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class WfApprovalTaskServiceImpl extends ServiceImpl<WfApprovalTaskMapper, WfApprovalTask> implements WfApprovalTaskService {

    private final WfProcessInstanceService processInstanceService;
    private final AiRecommendationService aiRecommendationService;
    private final WfApprovalHistoryService approvalHistoryService;
    private final WfNodeConfigService nodeConfigService;

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

    @Override
    public List<WfApprovalTask> listTodoByUserId(Long userId) {
        LambdaQueryWrapper<WfApprovalTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfApprovalTask::getAssigneeId, userId);
        wrapper.eq(WfApprovalTask::getTaskStatus, TaskStatusEnum.PENDING.getCode());
        wrapper.eq(WfApprovalTask::getNodeType, 1);
        wrapper.orderByDesc(WfApprovalTask::getCreateTime);
        return this.list(wrapper);
    }

    @Override
    public WfApprovalTask getByInstanceIdAndNodeId(Long instanceId, String nodeId) {
        LambdaQueryWrapper<WfApprovalTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfApprovalTask::getInstanceId, instanceId);
        wrapper.eq(WfApprovalTask::getNodeId, nodeId);
        wrapper.eq(WfApprovalTask::getTaskStatus, TaskStatusEnum.PENDING.getCode());
        wrapper.orderByDesc(WfApprovalTask::getId);
        wrapper.last("LIMIT 1");
        return this.getOne(wrapper, false);
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
            vo.setStartUserName(instance.getStartUserName());
            vo.setStartUserAvatar(instance.getStartUserAvatar());
            vo.setStartDeptName(instance.getStartDeptName());
            vo.setInstanceNo(instance.getInstanceNo());
            vo.setProcessName(instance.getProcessName());
            vo.setRejectCount(instance.getRejectCount() != null ? instance.getRejectCount() : 0);
            vo.setMaxRejectCount(instance.getMaxRejectCount() != null ? instance.getMaxRejectCount() : 5);
            vo.setFormData(instance.getFormData());
            vo.setBusinessLineName(instance.getBusinessLineName());
            vo.setCategoryName(instance.getCategoryName());
        }

        if (TaskStatusEnum.PENDING.getCode().equals(task.getTaskStatus())) {
            if (instance != null) {
                WfNodeConfig nodeConfig = nodeConfigService.getByNodeId(
                        instance.getProcessVersionId(), task.getNodeId());
                if (nodeConfig != null) {
                    vo.setCanAddSign(nodeConfig.getCanAddSign() != null && nodeConfig.getCanAddSign() == 1);
                    vo.setCanTransfer(nodeConfig.getCanTransfer() != null && nodeConfig.getCanTransfer() == 1);
                    vo.setCanDelegate(nodeConfig.getCanDelegate() != null && nodeConfig.getCanDelegate() == 1);
                    vo.setNeedSignature(nodeConfig.getNeedSignature() != null && nodeConfig.getNeedSignature() == 1);
                    vo.setNeedComment(nodeConfig.getNeedComment() != null && nodeConfig.getNeedComment() == 1);
                    vo.setCanReject(true);
                } else {
                    vo.setCanAddSign(false);
                    vo.setCanTransfer(false);
                    vo.setCanDelegate(false);
                    vo.setNeedSignature(false);
                    vo.setNeedComment(false);
                    vo.setCanReject(true);
                }
            }

            List<String> rejectableNodeIds = calcRejectableNodeIds(task.getInstanceId(), task.getNodeId());
            vo.setRejectableNodeIds(rejectableNodeIds);

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

    private List<String> calcRejectableNodeIds(Long instanceId, String currentNodeId) {
        Set<String> seenNodeIds = new HashSet<>();
        List<String> result = new ArrayList<>();
        List<WfApprovalHistory> historyList = approvalHistoryService.listValidByInstanceId(instanceId);
        for (WfApprovalHistory h : historyList) {
            if (h.getNodeId() == null) continue;
            if (h.getNodeId().equals(currentNodeId)) continue;
            if (h.getActivityType() != null && h.getActivityType() == 1) continue;
            if (h.getActivityType() == null
                    || (h.getActivityType() != 2 && h.getActivityType() != 7)) continue;
            if (seenNodeIds.contains(h.getNodeId())) continue;
            seenNodeIds.add(h.getNodeId());
            result.add(h.getNodeId());
        }
        return result;
    }
}
