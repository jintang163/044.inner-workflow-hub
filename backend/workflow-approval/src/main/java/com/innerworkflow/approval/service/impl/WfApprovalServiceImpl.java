package com.innerworkflow.approval.service.impl;

import com.innerworkflow.approval.dto.*;
import com.innerworkflow.approval.entity.*;
import com.innerworkflow.approval.service.*;
import com.innerworkflow.approval.vo.*;
import com.innerworkflow.auth.entity.SysUser;
import com.innerworkflow.auth.service.SysUserService;
import com.innerworkflow.bpmn.entity.WfNodeConfig;
import com.innerworkflow.bpmn.entity.WfProcessDefinition;
import com.innerworkflow.bpmn.entity.WfProcessVersion;
import com.innerworkflow.bpmn.service.WfNodeConfigService;
import com.innerworkflow.bpmn.service.WfProcessDefinitionService;
import com.innerworkflow.bpmn.service.WfProcessVersionService;
import com.innerworkflow.common.dto.LoginUserDTO;
import com.innerworkflow.common.enums.*;
import com.innerworkflow.common.exception.BusinessException;
import com.innerworkflow.common.util.JsonUtils;
import com.innerworkflow.common.util.SecurityUtils;
import com.innerworkflow.notify.dto.NotifySendDTO;
import com.innerworkflow.notify.enums.EventTypeEnum;
import com.innerworkflow.notify.service.WfNotifyService;
import com.innerworkflow.common.entity.WfAiRecommendation;
import com.innerworkflow.common.mapper.WfAiRecommendationMapper;
import com.innerworkflow.common.service.AiRecommendationService;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.*;
import org.flowable.engine.*;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class WfApprovalServiceImpl implements WfApprovalService {

    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final HistoryService historyService;
    private final RepositoryService repositoryService;
    private final ManagementService managementService;

    private final WfProcessInstanceService processInstanceService;
    private final WfApprovalTaskService approvalTaskService;
    private final WfApprovalHistoryService approvalHistoryService;
    private final WfCcTaskService ccTaskService;
    private final WfAttachmentService attachmentService;
    private final WfTaskRelationService taskRelationService;

    private final WfProcessDefinitionService processDefinitionService;
    private final WfProcessVersionService processVersionService;
    private final WfNodeConfigService nodeConfigService;
    private final WfNotifyService notifyService;
    private final AiRecommendationService aiRecommendationService;
    private final WfAiRecommendationMapper aiRecommendationMapper;
    private final WfProcessInstanceRelationService processInstanceRelationService;
    private final WfDelegationService delegationService;
    private final WfTransferRecordService transferRecordService;
    private final SysUserService sysUserService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String startProcess(WfStartProcessDTO dto) {
        WfProcessDefinition processDef = processDefinitionService.getByProcessKey(dto.getProcessKey());
        if (processDef == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "流程定义不存在");
        }
        if (processDef.getProcessStatus() != 1) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "流程未发布");
        }

        WfProcessVersion currentVersion = processVersionService.getCurrentVersion(processDef.getId());
        if (currentVersion == null || currentVersion.getFlowableProcessDefId() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "流程未部署");
        }

        Long userId = SecurityUtils.getCurrentUserId();
        String instanceNo = generateInstanceNo();

        Map<String, Object> variables = new HashMap<>();
        variables.put("startUserId", userId);
        variables.put("instanceNo", instanceNo);
        variables.put("title", dto.getTitle());
        variables.put("formData", dto.getFormData());

        if (dto.getFormData() != null && !dto.getFormData().isEmpty()) {
            for (Map.Entry<String, Object> entry : dto.getFormData().entrySet()) {
                variables.put(entry.getKey(), entry.getValue());
            }
        }

        ProcessInstance flowableInstance = runtimeService.startProcessInstanceById(
                currentVersion.getFlowableProcessDefId(),
                instanceNo,
                variables
        );

        WfProcessInstance instance = new WfProcessInstance();
        instance.setInstanceNo(instanceNo);
        instance.setProcessDefinitionId(processDef.getId());
        instance.setProcessKey(processDef.getProcessKey());
        instance.setProcessName(processDef.getProcessName());
        instance.setProcessVersionId(currentVersion.getId());
        instance.setFlowableProcessInstId(flowableInstance.getId());
        instance.setFlowableProcessDefId(currentVersion.getFlowableProcessDefId());
        instance.setBusinessLineId(processDef.getBusinessLineId());
        instance.setCategoryId(processDef.getCategoryId());
        instance.setTitle(dto.getTitle());
        instance.setFormId(currentVersion.getFormId());
        instance.setFormVersion(currentVersion.getFormVersion());
        instance.setFormData(dto.getFormData());
        instance.setInstanceStatus(InstanceStatusEnum.APPROVING.getCode());
        instance.setStartUserId(userId);
        instance.setStartUserName(SecurityUtils.getCurrentRealName());
        instance.setStartUserAvatar(SecurityUtils.getCurrentUserAvatar());
        instance.setStartDeptId(SecurityUtils.getCurrentDeptId());
        instance.setStartDeptName(SecurityUtils.getCurrentDeptName());
        instance.setStartTime(LocalDateTime.now());
        instance.setPriority(dto.getPriority() != null ? dto.getPriority() : 0);
        instance.setRejectCount(0);
        instance.setMaxRejectCount(5);
        instance.setFormDataVersion(1);
        processInstanceService.save(instance);

        saveApprovalHistory(instance.getId(), null, null, "start", "发起申请",
                HistoryActivityTypeEnum.START.getCode(), userId, SecurityUtils.getCurrentRealName(),
                null, null, null, null, dto.getRemark(), null, null, 0L, LocalDateTime.now());

        if (dto.getCcUserIds() != null && !dto.getCcUserIds().isEmpty()) {
            createCcTasks(instance.getId(), processDef.getProcessKey(), dto.getCcUserIds(), null, null);
        }

        if (dto.getAttachmentIds() != null && !dto.getAttachmentIds().isEmpty()) {
            attachmentService.updateBizId(dto.getAttachmentIds(), "FORM", instanceNo);
        }

        syncTasksFromFlowable(instance.getId(), flowableInstance.getId());

        log.info("流程发起成功, instanceNo={}, processKey={}, startUserId={}",
                instanceNo, dto.getProcessKey(), userId);

        return instanceNo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(WfApprovalActionDTO dto) {
        Task task = taskService.createTaskQuery().taskId(dto.getTaskId()).singleResult();
        if (task == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "任务不存在");
        }

        Long userId = SecurityUtils.getCurrentUserId();
        if (!userId.toString().equals(task.getAssignee())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作此任务");
        }

        WfApprovalTask approvalTask = approvalTaskService.getByFlowableTaskId(dto.getTaskId());
        if (approvalTask == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "审批任务不存在");
        }
        if (!TaskStatusEnum.PENDING.getCode().equals(approvalTask.getTaskStatus())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "任务已处理");
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", true);
        variables.put("approveUserId", userId);
        if (StrUtil.isNotBlank(dto.getActionRemark())) {
            variables.put("comment", dto.getActionRemark());
        }

        updateMultiInstanceSignCounters(task, true);

        taskService.addComment(dto.getTaskId(), task.getProcessInstanceId(), "AGREE",
                dto.getActionRemark() != null ? dto.getActionRemark() : "同意");

        taskService.complete(dto.getTaskId(), variables);

        updateApprovalTask(approvalTask, TaskActionEnum.AGREE.getCode(), dto.getActionRemark(),
                userId, dto.getAttachmentIds(), dto.getSignatureUrl());

        saveApprovalHistory(approvalTask.getInstanceId(), null, task.getTaskDefinitionKey(), task.getName(),
                HistoryActivityTypeEnum.APPROVE.getCode(), userId, SecurityUtils.getCurrentRealName(),
                null, null, null, null, dto.getActionRemark(), dto.getSignatureUrl(),
                dto.getAttachmentIds(), ChronoUnit.MILLIS.between(approvalTask.getAssignTime(), LocalDateTime.now()),
                LocalDateTime.now());

        syncTasksFromFlowable(approvalTask.getInstanceId(), task.getProcessInstanceId());

        updateInstanceStatus(task.getProcessInstanceId());

        recordAiAdoption(approvalTask, TaskActionEnum.AGREE.getCode());

        log.info("审批同意, taskId={}, instanceId={}, userId={}", dto.getTaskId(), task.getProcessInstanceId(), userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(WfApprovalActionDTO dto) {
        Task task = taskService.createTaskQuery().taskId(dto.getTaskId()).singleResult();
        if (task == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "任务不存在");
        }

        Long userId = SecurityUtils.getCurrentUserId();
        if (!userId.toString().equals(task.getAssignee())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作此任务");
        }

        WfApprovalTask approvalTask = approvalTaskService.getByFlowableTaskId(dto.getTaskId());
        if (approvalTask == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "审批任务不存在");
        }
        if (!TaskStatusEnum.PENDING.getCode().equals(approvalTask.getTaskStatus())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "任务已处理");
        }

        WfProcessInstance instance = processInstanceService.getById(approvalTask.getInstanceId());

        try {
            handleParallelGatewayRejection(instance, task, userId, dto);
        } catch (Exception e) {
            log.error("处理并行网关驳回失败, taskId={}, error={}", dto.getTaskId(), e.getMessage(), e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "处理驳回失败: " + e.getMessage());
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", false);
        variables.put("rejectUserId", userId);
        if (StrUtil.isNotBlank(dto.getActionRemark())) {
            variables.put("comment", dto.getActionRemark());
        }

        updateMultiInstanceSignCounters(task, false);

        taskService.addComment(dto.getTaskId(), task.getProcessInstanceId(), "REJECT",
                dto.getActionRemark() != null ? dto.getActionRemark() : "拒绝");

        taskService.complete(dto.getTaskId(), variables);

        updateApprovalTask(approvalTask, TaskActionEnum.REJECT.getCode(), dto.getActionRemark(),
                userId, dto.getAttachmentIds(), dto.getSignatureUrl());

        saveApprovalHistory(approvalTask.getInstanceId(), null, task.getTaskDefinitionKey(), task.getName(),
                HistoryActivityTypeEnum.REJECT.getCode(), userId, SecurityUtils.getCurrentRealName(),
                null, null, null, null, dto.getActionRemark(), dto.getSignatureUrl(),
                dto.getAttachmentIds(), ChronoUnit.MILLIS.between(approvalTask.getAssignTime(), LocalDateTime.now()),
                LocalDateTime.now());

        syncTasksFromFlowable(approvalTask.getInstanceId(), task.getProcessInstanceId());

        updateInstanceStatus(task.getProcessInstanceId());

        recordAiAdoption(approvalTask, TaskActionEnum.REJECT.getCode());

        log.info("审批拒绝, taskId={}, instanceId={}, userId={}", dto.getTaskId(), task.getProcessInstanceId(), userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transfer(WfTransferDTO dto) {
        Task task = taskService.createTaskQuery().taskId(dto.getTaskId()).singleResult();
        if (task == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "任务不存在");
        }

        Long userId = SecurityUtils.getCurrentUserId();
        if (!userId.toString().equals(task.getAssignee())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作此任务");
        }

        SysUser targetUser = sysUserService.getById(dto.getTargetUserId());
        if (targetUser == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "目标用户不存在");
        }
        String targetUserName = targetUser.getRealName();

        WfApprovalTask approvalTask = approvalTaskService.getByFlowableTaskId(dto.getTaskId());
        if (approvalTask == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "审批任务不存在");
        }

        taskService.setAssignee(dto.getTaskId(), dto.getTargetUserId().toString());

        approvalTask.setTaskStatus(TaskStatusEnum.TRANSFERRED.getCode());
        approvalTask.setAction(TaskActionEnum.TRANSFER.getCode());
        approvalTask.setActionRemark(dto.getActionRemark());
        approvalTask.setActionUserId(userId);
        approvalTask.setActionTime(LocalDateTime.now());
        approvalTaskService.updateById(approvalTask);

        WfApprovalTask newTask = new WfApprovalTask();
        newTask.setTaskNo(generateTaskNo());
        newTask.setInstanceId(approvalTask.getInstanceId());
        newTask.setFlowableTaskId(task.getId());
        newTask.setFlowableExecutionId(task.getExecutionId());
        newTask.setProcessKey(approvalTask.getProcessKey());
        newTask.setNodeId(approvalTask.getNodeId());
        newTask.setNodeName(approvalTask.getNodeName());
        newTask.setNodeType(approvalTask.getNodeType());
        newTask.setApproveType(approvalTask.getApproveType());
        newTask.setMultiInstanceFlag(approvalTask.getMultiInstanceFlag());
        newTask.setAssigneeId(dto.getTargetUserId());
        newTask.setAssignTime(LocalDateTime.now());
        newTask.setTaskStatus(TaskStatusEnum.PENDING.getCode());
        newTask.setSourceType(3);
        newTask.setSourceTaskId(approvalTask.getId());
        approvalTaskService.save(newTask);

        WfTaskRelation relation = new WfTaskRelation();
        relation.setParentTaskId(approvalTask.getId());
        relation.setChildTaskId(newTask.getId());
        relation.setRelationType(3);
        relation.setOperatorId(userId);
        taskRelationService.save(relation);

        saveApprovalHistory(approvalTask.getInstanceId(), null, task.getTaskDefinitionKey(), task.getName(),
                HistoryActivityTypeEnum.TRANSFER.getCode(), userId, SecurityUtils.getCurrentRealName(),
                dto.getTargetUserId(), targetUserName, null, null, dto.getActionRemark(), null,
                null, null, LocalDateTime.now());

        transferRecordService.createTransferRecord(
                approvalTask.getInstanceId(),
                approvalTask.getId(),
                dto.getTransferType() != null ? dto.getTransferType() : TransferTypeEnum.MANUAL.getCode(),
                userId,
                SecurityUtils.getCurrentRealName(),
                dto.getTargetUserId(),
                targetUserName,
                dto.getActionRemark(),
                null
        );

        sendTransferNotify(approvalTask, userId, dto.getTargetUserId(), dto.getActionRemark());

        log.info("任务转审, taskId={}, targetUserId={}, userId={}", dto.getTaskId(), dto.getTargetUserId(), userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addSign(WfAddSignDTO dto) {
        Task task = taskService.createTaskQuery().taskId(dto.getTaskId()).singleResult();
        if (task == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "任务不存在");
        }

        Long userId = SecurityUtils.getCurrentUserId();
        if (!userId.toString().equals(task.getAssignee())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作此任务");
        }

        WfApprovalTask approvalTask = approvalTaskService.getByFlowableTaskId(dto.getTaskId());
        if (approvalTask == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "审批任务不存在");
        }

        WfProcessInstance instance = processInstanceService.getById(approvalTask.getInstanceId());
        WfNodeConfig nodeConfig = null;
        if (instance != null) {
            nodeConfig = nodeConfigService.getByNodeId(instance.getProcessVersionId(), task.getTaskDefinitionKey());
        }
        if (nodeConfig == null || nodeConfig.getCanAddSign() == 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "当前节点不允许加签");
        }

        if (dto.getTargetUserIds() == null || dto.getTargetUserIds().isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "加签人不能为空");
        }

        int signType = dto.getSignType() != null ? dto.getSignType() : 1;
        int relationType = signType == 1 ? 1 : 2;

        for (Long targetUserId : dto.getTargetUserIds()) {
            Task newTask = taskService.newTask();
            newTask.setName(task.getName() + "(加签)");
            newTask.setTaskDefinitionKey(task.getTaskDefinitionKey() + "_addSign");
            newTask.setProcessInstanceId(task.getProcessInstanceId());
            newTask.setExecutionId(task.getExecutionId());
            newTask.setAssignee(targetUserId.toString());
            newTask.setPriority(task.getPriority());
            taskService.saveTask(newTask);

            WfApprovalTask newApprovalTask = new WfApprovalTask();
            newApprovalTask.setTaskNo(generateTaskNo());
            newApprovalTask.setInstanceId(approvalTask.getInstanceId());
            newApprovalTask.setFlowableTaskId(newTask.getId());
            newApprovalTask.setFlowableExecutionId(task.getExecutionId());
            newApprovalTask.setProcessKey(approvalTask.getProcessKey());
            newApprovalTask.setNodeId(task.getTaskDefinitionKey());
            newApprovalTask.setNodeName(task.getName() + "(加签)");
            newApprovalTask.setNodeType(1);
            newApprovalTask.setAssigneeId(targetUserId);
            newApprovalTask.setAssignTime(LocalDateTime.now());
            newApprovalTask.setTaskStatus(TaskStatusEnum.PENDING.getCode());
            newApprovalTask.setSourceType(2);
            newApprovalTask.setSourceTaskId(approvalTask.getId());
            approvalTaskService.save(newApprovalTask);

            WfTaskRelation relation = new WfTaskRelation();
            relation.setParentTaskId(approvalTask.getId());
            relation.setChildTaskId(newApprovalTask.getId());
            relation.setRelationType(relationType);
            relation.setOperatorId(userId);
            taskRelationService.save(relation);
        }

        saveApprovalHistory(approvalTask.getInstanceId(), null, task.getTaskDefinitionKey(), task.getName(),
                HistoryActivityTypeEnum.ADD_SIGN.getCode(), userId, SecurityUtils.getCurrentRealName(),
                null, null, null, null, dto.getActionRemark(), null,
                null, null, LocalDateTime.now());

        log.info("任务加签, taskId={}, targetUserIds={}, userId={}", dto.getTaskId(), dto.getTargetUserIds(), userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delegate(WfDelegateDTO dto) {
        Task task = taskService.createTaskQuery().taskId(dto.getTaskId()).singleResult();
        if (task == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "任务不存在");
        }

        Long userId = SecurityUtils.getCurrentUserId();
        if (!userId.toString().equals(task.getAssignee())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作此任务");
        }

        WfApprovalTask approvalTask = approvalTaskService.getByFlowableTaskId(dto.getTaskId());
        if (approvalTask == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "审批任务不存在");
        }

        taskService.delegateTask(dto.getTaskId(), dto.getTargetUserId().toString());

        approvalTask.setTaskStatus(TaskStatusEnum.DELEGATED.getCode());
        approvalTask.setAction(TaskActionEnum.DELEGATE.getCode());
        approvalTask.setActionRemark(dto.getActionRemark());
        approvalTask.setActionUserId(dto.getTargetUserId());
        approvalTask.setActionTime(LocalDateTime.now());
        approvalTaskService.updateById(approvalTask);

        saveApprovalHistory(approvalTask.getInstanceId(), null, task.getTaskDefinitionKey(), task.getName(),
                HistoryActivityTypeEnum.DELEGATE.getCode(), userId, SecurityUtils.getCurrentRealName(),
                dto.getTargetUserId(), null, null, null, dto.getActionRemark(), null,
                null, null, LocalDateTime.now());

        log.info("任务委派, taskId={}, targetUserId={}, userId={}", dto.getTaskId(), dto.getTargetUserId(), userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectToNode(WfRejectDTO dto) {
        Task task = taskService.createTaskQuery().taskId(dto.getTaskId()).singleResult();
        if (task == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "任务不存在");
        }

        Long userId = SecurityUtils.getCurrentUserId();
        if (!userId.toString().equals(task.getAssignee())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作此任务");
        }

        WfApprovalTask approvalTask = approvalTaskService.getByFlowableTaskId(dto.getTaskId());
        if (approvalTask == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "审批任务不存在");
        }
        if (!TaskStatusEnum.PENDING.getCode().equals(approvalTask.getTaskStatus())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "任务已处理");
        }

        WfProcessInstance instance = processInstanceService.getById(approvalTask.getInstanceId());
        if (instance == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "流程实例不存在");
        }

        int currentRejectCount = instance.getRejectCount() != null ? instance.getRejectCount() : 0;
        int maxRejectCount = instance.getMaxRejectCount() != null ? instance.getMaxRejectCount() : 5;
        if (currentRejectCount >= maxRejectCount) {
            throw new BusinessException(ResultCode.PARAM_ERROR,
                    "已达到最大驳回次数（" + maxRejectCount + "次），无法继续驳回");
        }

        List<String> rejectableNodeIds = calcRejectableNodeIds(approvalTask.getInstanceId(), task.getTaskDefinitionKey());

        String targetNodeId = dto.getTargetNodeId();
        if (StrUtil.isBlank(targetNodeId)) {
            WfNodeConfig nodeConfig = nodeConfigService.getByNodeId(
                    instance.getProcessVersionId(), task.getTaskDefinitionKey());
            if (nodeConfig != null && nodeConfig.getRefuseStrategy() != null) {
                if (nodeConfig.getRefuseStrategy() == 2) {
                    targetNodeId = getPreviousNodeId(approvalTask.getInstanceId(), task.getTaskDefinitionKey());
                } else if (nodeConfig.getRefuseStrategy() == 3) {
                    targetNodeId = nodeConfig.getRefuseTargetNodeId();
                }
            }
        }

        if (StrUtil.isBlank(targetNodeId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "驳回目标节点不能为空");
        }

        if (!"start".equals(targetNodeId) && !rejectableNodeIds.contains(targetNodeId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不允许驳回到该节点");
        }

        boolean resetFormData = Boolean.TRUE.equals(dto.getResetFormData());
        int currentFormVersion = instance.getFormDataVersion() != null ? instance.getFormDataVersion() : 1;
        if (resetFormData) {
            instance.setFormData(null);
        }
        instance.setFormDataVersion(currentFormVersion + 1);
        instance.setRejectCount(currentRejectCount + 1);
        processInstanceService.updateById(instance);

        Map<String, Object> variables = new HashMap<>();
        variables.put("rejectCount", currentRejectCount + 1);
        variables.put("maxRejectCount", maxRejectCount);
        variables.put("formDataVersion", currentFormVersion + 1);
        variables.put("resetFormData", resetFormData);
        variables.put("rejectTargetNodeId", targetNodeId);
        if (StrUtil.isNotBlank(dto.getActionRemark())) {
            variables.put("actionRemark", dto.getActionRemark());
            variables.put("comment", dto.getActionRemark());
        }
        if (!resetFormData && instance.getFormData() != null) {
            variables.put("formData", instance.getFormData());
        } else {
            variables.put("formData", null);
        }
        runtimeService.setVariables(task.getProcessInstanceId(), variables);

        taskService.addComment(dto.getTaskId(), task.getProcessInstanceId(), "REJECT_TO_NODE",
                dto.getActionRemark() != null ? dto.getActionRemark() : "驳回到" + targetNodeId);

        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(task.getProcessInstanceId())
                .moveActivityIdTo(task.getTaskDefinitionKey(), targetNodeId)
                .changeState();

        updateApprovalTask(approvalTask, TaskActionEnum.SEND_BACK.getCode(), dto.getActionRemark(),
                userId, dto.getAttachmentIds(), dto.getSignatureUrl());

        approvalHistoryService.markInvalidByInstanceIdAndNodeId(approvalTask.getInstanceId(), task.getTaskDefinitionKey());

        String targetNodeName = dto.getTargetNodeName();
        if (StrUtil.isBlank(targetNodeName) && !"start".equals(targetNodeId)) {
            WfNodeConfig targetNodeConfig = nodeConfigService.getByNodeId(
                    instance.getProcessVersionId(), targetNodeId);
            if (targetNodeConfig != null) {
                targetNodeName = targetNodeConfig.getNodeName();
            }
        }
        if (StrUtil.isBlank(targetNodeName) && "start".equals(targetNodeId)) {
            targetNodeName = "发起节点";
        }

        saveApprovalHistory(approvalTask.getInstanceId(), null, task.getTaskDefinitionKey(), task.getName(),
                HistoryActivityTypeEnum.REJECT.getCode(), userId, SecurityUtils.getCurrentRealName(),
                null, null, targetNodeId, targetNodeName, dto.getActionRemark(), dto.getSignatureUrl(),
                dto.getAttachmentIds(), ChronoUnit.MILLIS.between(approvalTask.getAssignTime(), LocalDateTime.now()),
                LocalDateTime.now());

        syncTasksFromFlowable(approvalTask.getInstanceId(), task.getProcessInstanceId());

        recordAiAdoption(approvalTask, TaskActionEnum.REJECT.getCode());

        log.info("任务驳回, taskId={}, targetNodeId={}, targetNodeName={}, resetFormData={}, rejectCount={}, userId={}",
                dto.getTaskId(), targetNodeId, targetNodeName, resetFormData, currentRejectCount + 1, userId);
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdraw(WfWithdrawDTO dto) {
        WfProcessInstance instance = processInstanceService.getById(Long.valueOf(dto.getInstanceId()));
        if (instance == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "流程实例不存在");
        }

        Long userId = SecurityUtils.getCurrentUserId();
        if (!userId.equals(instance.getStartUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只有发起人才能撤回");
        }

        if (!InstanceStatusEnum.APPROVING.getCode().equals(instance.getInstanceStatus())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "流程已结束，无法撤回");
        }

        runtimeService.deleteProcessInstance(instance.getFlowableProcessInstId(), "撤回:" + dto.getActionRemark());

        instance.setInstanceStatus(InstanceStatusEnum.CANCELED.getCode());
        instance.setEndTime(LocalDateTime.now());
        instance.setDuration(ChronoUnit.MILLIS.between(instance.getStartTime(), LocalDateTime.now()));
        processInstanceService.updateById(instance);

        List<WfApprovalTask> tasks = approvalTaskService.listTodoByInstanceId(instance.getId());
        for (WfApprovalTask task : tasks) {
            task.setTaskStatus(TaskStatusEnum.CANCELED.getCode());
            approvalTaskService.updateById(task);
        }

        saveApprovalHistory(instance.getId(), null, null, null,
                HistoryActivityTypeEnum.WITHDRAW.getCode(), userId, SecurityUtils.getCurrentRealName(),
                null, null, null, null, dto.getActionRemark(), null,
                null, ChronoUnit.MILLIS.between(instance.getStartTime(), LocalDateTime.now()),
                LocalDateTime.now());

        log.info("流程撤回, instanceId={}, userId={}", dto.getInstanceId(), userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchApprove(WfBatchApprovalDTO dto) {
        if (dto.getTaskIds() == null || dto.getTaskIds().isEmpty()) {
            return;
        }

        for (String taskId : dto.getTaskIds()) {
            try {
                if (TaskActionEnum.AGREE.getCode().equals(dto.getAction())) {
                    WfApprovalActionDTO actionDTO = new WfApprovalActionDTO();
                    actionDTO.setTaskId(taskId);
                    actionDTO.setActionRemark(dto.getActionRemark());
                    approve(actionDTO);
                } else if (TaskActionEnum.REJECT.getCode().equals(dto.getAction())) {
                    WfApprovalActionDTO actionDTO = new WfApprovalActionDTO();
                    actionDTO.setTaskId(taskId);
                    actionDTO.setActionRemark(dto.getActionRemark());
                    reject(actionDTO);
                }
            } catch (Exception e) {
                log.error("批量审批失败, taskId={}, error={}", taskId, e.getMessage());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchTransfer(WfBatchTransferDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<String> taskIds = dto.getTaskIds();

        if (Boolean.TRUE.equals(dto.getTransferAll())) {
            List<WfApprovalTask> todoTasks = approvalTaskService.listTodoByUserId(userId);
            taskIds = todoTasks.stream()
                    .map(WfApprovalTask::getFlowableTaskId)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList());
        }

        if (taskIds == null || taskIds.isEmpty()) {
            return;
        }

        SysUser targetUser = sysUserService.getById(dto.getTargetUserId());
        if (targetUser == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "目标用户不存在");
        }
        String targetUserName = targetUser.getRealName();

        int successCount = 0;
        for (String taskId : taskIds) {
            try {
                WfTransferDTO transferDTO = new WfTransferDTO();
                transferDTO.setTaskId(taskId);
                transferDTO.setTargetUserId(dto.getTargetUserId());
                transferDTO.setTargetUserName(targetUserName);
                transferDTO.setActionRemark(dto.getActionRemark());
                transferDTO.setTransferType(TransferTypeEnum.BATCH.getCode());
                transfer(transferDTO);

                successCount++;
            } catch (Exception e) {
                log.error("批量转审失败, taskId={}, error={}", taskId, e.getMessage());
            }
        }

        log.info("批量转审完成, 共{}个任务, 成功{}个", taskIds.size(), successCount);
    }

    @Override
    public WfProcessDetailVO getProcessDetail(Long instanceId) {
        WfProcessInstance instance = processInstanceService.getById(instanceId);
        if (instance == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "流程实例不存在");
        }

        WfProcessDetailVO detailVO = new WfProcessDetailVO();

        WfProcessInstanceVO instanceVO = processInstanceService.getDetailById(instanceId);
        detailVO.setInstance(instanceVO);

        List<WfApprovalTask> currentTasks = approvalTaskService.listTodoByInstanceId(instanceId);
        List<WfApprovalTaskVO> currentTaskVOs = currentTasks.stream()
                .map(this::convertTaskToVO)
                .collect(Collectors.toList());
        detailVO.setCurrentTasks(currentTaskVOs);

        List<WfApprovalHistory> historyList = approvalHistoryService.listValidByInstanceId(instanceId);
        List<WfApprovalHistoryVO> historyVOs = historyList.stream()
                .map(this::convertHistoryToVO)
                .collect(Collectors.toList());
        detailVO.setHistoryList(historyVOs);

        BpmnModel bpmnModel = null;
        try {
            ProcessDefinition processDef = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionId(instance.getFlowableProcessDefId())
                    .singleResult();
            if (processDef != null) {
                bpmnModel = repositoryService.getBpmnModel(processDef.getId());
                detailVO.setBpmnXml(convertBpmnModelToXml(bpmnModel));
            }
        } catch (Exception e) {
            log.error("获取流程图失败, instanceId={}, error={}", instanceId, e.getMessage());
        }

        Set<String> highLightedNodeIds = new HashSet<>();
        Set<String> highLightedFlowIds = new HashSet<>();
        List<HistoricActivityInstance> historicActivities = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(instance.getFlowableProcessInstId())
                .list();
        for (HistoricActivityInstance activity : historicActivities) {
            if (activity.getEndTime() != null) {
                if ("sequenceFlow".equals(activity.getActivityType())) {
                    highLightedFlowIds.add(activity.getActivityId());
                } else {
                    highLightedNodeIds.add(activity.getActivityId());
                }
            }
        }
        List<Task> currentFlowableTasks = taskService.createTaskQuery()
                .processInstanceId(instance.getFlowableProcessInstId())
                .list();
        for (Task task : currentFlowableTasks) {
            highLightedNodeIds.add(task.getTaskDefinitionKey());
        }
        detailVO.setHighLightedNodeIds(new ArrayList<>(highLightedNodeIds));
        detailVO.setHighLightedFlowIds(new ArrayList<>(highLightedFlowIds));

        detailVO.setFormData(instance.getFormData());

        List<WfCcTaskVO> ccTaskList = ccTaskService.listAllVOsByInstanceId(instanceId);
        detailVO.setCcTaskList(ccTaskList);
        detailVO.setCcUnreadCount(ccTaskService.countUnreadByInstanceId(instanceId));

        if (bpmnModel != null) {
            try {
                List<WfParallelProgressVO> parallelProgressList = calculateParallelProgress(
                        bpmnModel, instance, historicActivities, currentFlowableTasks);
                detailVO.setParallelProgressList(parallelProgressList);
            } catch (Exception e) {
                log.error("计算并行进度失败, instanceId={}, error={}", instanceId, e.getMessage(), e);
            }
        }

        try {
            List<WfMultiInstanceSignVO> multiInstanceSignList = buildMultiInstanceSignList(instance);
            detailVO.setMultiInstanceSignList(multiInstanceSignList);
        } catch (Exception e) {
            log.error("构建会签节点状态失败, instanceId={}, error={}", instanceId, e.getMessage(), e);
        }

        try {
            WfTrackingMapVO trackingMap = buildTrackingMap(instance, historicActivities, bpmnModel);
            detailVO.setTrackingMap(trackingMap);
        } catch (Exception e) {
            log.error("构建审批跟踪地图失败, instanceId={}, error={}", instanceId, e.getMessage(), e);
        }

        List<WfProcessInstanceRelationVO> childProcessList = processInstanceRelationService.listByParentInstanceId(instanceId);
        detailVO.setChildProcessInstanceList(childProcessList);

        WfProcessInstanceRelation parentRelation = processInstanceRelationService.getByChildInstanceId(instanceId);
        if (parentRelation != null) {
            WfProcessInstanceRelationVO parentVO = new WfProcessInstanceRelationVO();
            org.springframework.beans.BeanUtils.copyProperties(parentRelation, parentVO);
            detailVO.setParentProcessInstance(parentVO);
        }

        calcButtonPermissions(detailVO, instance);

        return detailVO;
    }

    @Override
    public String getProcessDiagram(Long instanceId) {
        WfProcessInstance instance = processInstanceService.getById(instanceId);
        if (instance == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "流程实例不存在");
        }

        ProcessDefinition processDef = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(instance.getFlowableProcessDefId())
                .singleResult();

        if (processDef == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "流程定义不存在");
        }

        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDef.getId());
        return convertBpmnModelToXml(bpmnModel);
    }

    private void syncTasksFromFlowable(Long instanceId, String flowableInstanceId) {
        List<Task> tasks = taskService.createTaskQuery()
                .processInstanceId(flowableInstanceId)
                .list();

        WfProcessInstance instance = processInstanceService.getById(instanceId);
        if (instance != null) {
            List<String> currentNodeIds = new ArrayList<>();
            List<Long> currentApproverIds = new ArrayList<>();

            for (Task task : tasks) {
                if (!currentNodeIds.contains(task.getTaskDefinitionKey())) {
                    currentNodeIds.add(task.getTaskDefinitionKey());
                }
                Long assigneeId = parseAssigneeId(task.getAssignee());
                if (assigneeId != null && !currentApproverIds.contains(assigneeId)) {
                    currentApproverIds.add(assigneeId);
                }
            }
            instance.setCurrentNodeIds(currentNodeIds);
            instance.setCurrentApproverIds(currentApproverIds);
            processInstanceService.updateById(instance);
            log.info("同步流程实例状态, instanceId={}, currentNodeIds={}, currentApproverIds={}",
                    instanceId, currentNodeIds, currentApproverIds);
        }

        for (Task task : tasks) {
            WfApprovalTask existingTask = approvalTaskService.getByFlowableTaskId(task.getId());
            if (existingTask != null) {
                Long assigneeId = parseAssigneeId(task.getAssignee());
                if (assigneeId != null && !assigneeId.equals(existingTask.getAssigneeId())) {
                    existingTask.setAssigneeId(assigneeId);
                    existingTask.setAssignTime(LocalDateTime.now());
                    approvalTaskService.updateById(existingTask);
                }
                continue;
            }

            WfApprovalTask approvalTask = new WfApprovalTask();
            approvalTask.setTaskNo(generateTaskNo());
            approvalTask.setInstanceId(instanceId);
            approvalTask.setFlowableTaskId(task.getId());
            approvalTask.setFlowableExecutionId(task.getExecutionId());
            approvalTask.setProcessKey(task.getProcessDefinitionId().split(":")[0]);
            approvalTask.setNodeId(task.getTaskDefinitionKey());
            approvalTask.setNodeName(task.getName());
            approvalTask.setNodeType(1);
            approvalTask.setAssigneeId(parseAssigneeId(task.getAssignee()));
            approvalTask.setAssignTime(LocalDateTime.now());
            approvalTask.setTaskStatus(TaskStatusEnum.PENDING.getCode());
            approvalTask.setSourceType(1);
            approvalTask.setEscalateLevel(0);
            approvalTaskService.save(approvalTask);
            log.info("同步创建待办任务, flowableTaskId={}, instanceId={}", task.getId(), instanceId);

            handleDelegationForTask(approvalTask, task);
        }
    }

    private void handleDelegationForTask(WfApprovalTask approvalTask, Task flowableTask) {
        if (approvalTask.getAssigneeId() == null) {
            return;
        }

        try {
            WfDelegation delegation = delegationService.getActiveDelegation(
                    approvalTask.getAssigneeId(),
                    approvalTask.getProcessKey()
            );

            if (delegation != null) {
                log.info("检测到生效委托, 自动转审任务, taskId={}, delegatorId={}, delegateeId={}",
                        approvalTask.getId(), delegation.getDelegatorId(), delegation.getDelegateeId());

                taskService.setAssignee(flowableTask.getId(), delegation.getDelegateeId().toString());

                approvalTask.setTaskStatus(TaskStatusEnum.TRANSFERRED.getCode());
                approvalTask.setAction(TaskActionEnum.TRANSFER.getCode());
                approvalTask.setActionRemark("委托自动转审: " + delegation.getDelegationReason());
                approvalTask.setActionUserId(delegation.getDelegateeId());
                approvalTask.setActionTime(LocalDateTime.now());
                approvalTaskService.updateById(approvalTask);

                WfApprovalTask newTask = new WfApprovalTask();
                newTask.setTaskNo(generateTaskNo());
                newTask.setInstanceId(approvalTask.getInstanceId());
                newTask.setFlowableTaskId(flowableTask.getId());
                newTask.setFlowableExecutionId(flowableTask.getExecutionId());
                newTask.setProcessKey(approvalTask.getProcessKey());
                newTask.setNodeId(approvalTask.getNodeId());
                newTask.setNodeName(approvalTask.getNodeName());
                newTask.setNodeType(approvalTask.getNodeType());
                newTask.setApproveType(approvalTask.getApproveType());
                newTask.setMultiInstanceFlag(approvalTask.getMultiInstanceFlag());
                newTask.setAssigneeId(delegation.getDelegateeId());
                newTask.setAssignTime(LocalDateTime.now());
                newTask.setTaskStatus(TaskStatusEnum.PENDING.getCode());
                newTask.setSourceType(3);
                newTask.setSourceTaskId(approvalTask.getId());
                approvalTaskService.save(newTask);

                WfTaskRelation relation = new WfTaskRelation();
                relation.setParentTaskId(approvalTask.getId());
                relation.setChildTaskId(newTask.getId());
                relation.setRelationType(3);
                relation.setOperatorId(delegation.getDelegatorId());
                taskRelationService.save(relation);

                saveApprovalHistory(approvalTask.getInstanceId(), null, flowableTask.getTaskDefinitionKey(),
                        flowableTask.getName(),
                        HistoryActivityTypeEnum.TRANSFER.getCode(),
                        delegation.getDelegatorId(), delegation.getDelegatorName(),
                        delegation.getDelegateeId(), delegation.getDelegateeName(),
                        null, null,
                        "委托自动转审: " + delegation.getDelegationReason(),
                        null, null, null, LocalDateTime.now());

                transferRecordService.createTransferRecord(
                        approvalTask.getInstanceId(),
                        approvalTask.getId(),
                        TransferTypeEnum.DELEGATION.getCode(),
                        delegation.getDelegatorId(),
                        delegation.getDelegatorName(),
                        delegation.getDelegateeId(),
                        delegation.getDelegateeName(),
                        "委托自动转审: " + delegation.getDelegationReason(),
                        delegation.getId()
                );

                sendTransferNotify(newTask, delegation.getDelegatorId(),
                        delegation.getDelegateeId(),
                        "委托自动转审: " + delegation.getDelegationReason());
            }
        } catch (Exception e) {
            log.error("处理委托自动转审失败, taskId={}, error={}", approvalTask.getId(), e.getMessage(), e);
        }
    }

    private Long parseAssigneeId(String assignee) {
        if (StrUtil.isBlank(assignee)) {
            return null;
        }
        try {
            return Long.parseLong(assignee);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void updateApprovalTask(WfApprovalTask task, Integer action, String remark,
                                    Long actionUserId, List<Long> attachmentIds, String signatureUrl) {
        task.setAction(action);
        task.setActionRemark(remark);
        task.setActionUserId(actionUserId);
        task.setActionTime(LocalDateTime.now());
        task.setTaskStatus(TaskStatusEnum.DONE.getCode());
        task.setAttachmentIds(attachmentIds);
        task.setSignatureUrl(signatureUrl);
        if (task.getAssignTime() != null) {
            task.setActionDuration(ChronoUnit.MILLIS.between(task.getAssignTime(), LocalDateTime.now()));
        }
        approvalTaskService.updateById(task);
    }

    private void updateInstanceStatus(String flowableInstanceId) {
        HistoricProcessInstance historicInstance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(flowableInstanceId)
                .singleResult();

        if (historicInstance != null && historicInstance.getEndTime() != null) {
            WfProcessInstance instance = processInstanceService.getByFlowableInstId(flowableInstanceId);
            if (instance != null) {
                String deleteReason = historicInstance.getDeleteReason();
                if (deleteReason != null && deleteReason.contains("驳回")) {
                    instance.setInstanceStatus(InstanceStatusEnum.REJECTED.getCode());
                } else if (deleteReason != null && deleteReason.contains("撤回")) {
                    instance.setInstanceStatus(InstanceStatusEnum.CANCELED.getCode());
                } else {
                    instance.setInstanceStatus(InstanceStatusEnum.APPROVED.getCode());
                }
                instance.setEndTime(LocalDateTime.now());
                instance.setDuration(ChronoUnit.MILLIS.between(instance.getStartTime(), LocalDateTime.now()));
                processInstanceService.updateById(instance);

                saveApprovalHistory(instance.getId(), null, null, null,
                        HistoryActivityTypeEnum.PROCESS_END.getCode(), null, null,
                        null, null, null, null, null, null,
                        null, instance.getDuration(), LocalDateTime.now());
            }
        }
    }

    private void saveApprovalHistory(Long instanceId, String flowableActInstId, String nodeId, String nodeName,
                                     Integer activityType, Long operatorId, String operatorName,
                                     Long targetUserId, String targetUserName,
                                     String targetNodeId, String targetNodeName,
                                     String actionRemark, String signatureUrl,
                                     List<Long> attachmentIds, Long duration, LocalDateTime operateTime) {
        WfApprovalHistory history = new WfApprovalHistory();
        history.setInstanceId(instanceId);
        history.setFlowableActInstId(flowableActInstId);
        history.setNodeId(nodeId);
        history.setNodeName(nodeName);
        history.setActivityType(activityType);
        history.setOperatorId(operatorId);
        history.setOperatorName(operatorName);
        history.setTargetUserId(targetUserId);
        history.setTargetUserName(targetUserName);
        history.setTargetNodeId(targetNodeId);
        history.setTargetNodeName(targetNodeName);
        history.setActionRemark(actionRemark);
        history.setSignatureUrl(signatureUrl);
        history.setAttachmentIds(attachmentIds);
        history.setDuration(duration);
        history.setIsValid(1);
        history.setOperateTime(operateTime);
        approvalHistoryService.save(history);
    }

    private void createCcTasks(Long instanceId, String processKey, List<Long> userIds, String nodeId, String nodeName) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        WfCcAddDTO addDTO = new WfCcAddDTO();
        addDTO.setInstanceId(instanceId);
        addDTO.setCcUserIds(userIds);
        addDTO.setNodeId(nodeId);
        addDTO.setNodeName(nodeName);
        ccTaskService.addCcInternal(addDTO);
    }

    private String generateInstanceNo() {
        return "WF" + System.currentTimeMillis() + IdUtil.randomUUID().substring(0, 4).toUpperCase();
    }

    private String generateTaskNo() {
        return "TK" + System.currentTimeMillis() + IdUtil.randomUUID().substring(0, 4).toUpperCase();
    }

    private String getPreviousNodeId(Long instanceId, String currentNodeId) {
        List<WfApprovalHistory> historyList = approvalHistoryService.listValidByInstanceId(instanceId);
        String prevNodeId = null;
        for (WfApprovalHistory history : historyList) {
            if (currentNodeId.equals(history.getNodeId())) {
                break;
            }
            if (history.getActivityType() != null && history.getActivityType() == 2) {
                prevNodeId = history.getNodeId();
            }
        }
        return prevNodeId;
    }

    private String convertBpmnModelToXml(BpmnModel bpmnModel) {
        try {
            org.flowable.bpmn.converter.BpmnXMLConverter converter = new org.flowable.bpmn.converter.BpmnXMLConverter();
            byte[] bytes = converter.convertToXML(bpmnModel);
            return new String(bytes);
        } catch (Exception e) {
            log.error("BPMN模型转XML失败: {}", e.getMessage());
            return null;
        }
    }

    private WfApprovalTaskVO convertTaskToVO(WfApprovalTask task) {
        WfApprovalTaskVO vo = new WfApprovalTaskVO();
        org.springframework.beans.BeanUtils.copyProperties(task, vo);
        TaskStatusEnum statusEnum = TaskStatusEnum.getByCode(task.getTaskStatus());
        if (statusEnum != null) {
            vo.setTaskStatusName(statusEnum.getDesc());
        }
        TaskActionEnum actionEnum = TaskActionEnum.getByCode(task.getAction());
        if (actionEnum != null) {
            vo.setActionName(actionEnum.getDesc());
        }
        return vo;
    }

    private WfApprovalHistoryVO convertHistoryToVO(WfApprovalHistory history) {
        WfApprovalHistoryVO vo = new WfApprovalHistoryVO();
        org.springframework.beans.BeanUtils.copyProperties(history, vo);
        HistoryActivityTypeEnum typeEnum = HistoryActivityTypeEnum.getByCode(history.getActivityType());
        if (typeEnum != null) {
            vo.setActivityTypeName(typeEnum.getDesc());
        }
        return vo;
    }

    private void calcButtonPermissions(WfProcessDetailVO detailVO, WfProcessInstance instance) {
        Long userId = SecurityUtils.getCurrentUserIdOrNull();
        if (userId == null) {
            detailVO.setCanWithdraw(false);
            detailVO.setCanApprove(false);
            detailVO.setCanReject(false);
            detailVO.setCanTransfer(false);
            detailVO.setCanAddSign(false);
            detailVO.setCanDelegate(false);
            return;
        }

        boolean isStartUser = userId.equals(instance.getStartUserId());
        detailVO.setCanWithdraw(isStartUser && InstanceStatusEnum.APPROVING.getCode().equals(instance.getInstanceStatus()));

        boolean hasTodoTask = detailVO.getCurrentTasks() != null &&
                detailVO.getCurrentTasks().stream().anyMatch(t -> userId.equals(t.getAssigneeId()));
        detailVO.setCanApprove(hasTodoTask);
        detailVO.setCanReject(hasTodoTask);
        detailVO.setCanTransfer(hasTodoTask);
        detailVO.setCanAddSign(hasTodoTask);
        detailVO.setCanDelegate(hasTodoTask);
    }

    private void recordAiAdoption(WfApprovalTask task, Integer action) {
        if (task.getAiRecommendationId() == null) {
            return;
        }
        try {
            WfAiRecommendation rec = aiRecommendationMapper.selectById(task.getAiRecommendationId());
            if (rec != null && (rec.getAdopted() == null || rec.getAdopted() == 0)) {
                Integer adopted = (rec.getRecommendedAction() != null &&
                        ((rec.getRecommendedAction() == 1 && TaskActionEnum.AGREE.getCode().equals(action)) ||
                                (rec.getRecommendedAction() == 0 && TaskActionEnum.REJECT.getCode().equals(action))))
                        ? 1
                        : 2;
                aiRecommendationService.recordAdoption(rec.getId(), adopted);
            }
        } catch (Exception e) {
            log.warn("记录AI推荐采纳情况失败, taskId={}, error={}", task.getId(), e.getMessage());
        }
    }

    private void sendTransferNotify(WfApprovalTask approvalTask, Long sourceUserId,
                                    Long targetUserId, String transferReason) {
        try {
            WfProcessInstance instance = processInstanceService.getById(approvalTask.getInstanceId());
            if (instance == null) {
                return;
            }

            NotifySendDTO sendDTO = new NotifySendDTO();
            sendDTO.setEventType("TASK_TRANSFER");
            sendDTO.setBusinessType("WORKFLOW");
            sendDTO.setInstanceId(instance.getId());
            sendDTO.setTaskId(approvalTask.getId());
            sendDTO.setReceiverUserId(targetUserId);

            java.util.Map<String, Object> params = new java.util.HashMap<>();
            params.put("processTitle", instance.getTitle());
            params.put("instanceNo", instance.getInstanceNo());
            params.put("processKey", instance.getProcessKey());
            params.put("nodeName", approvalTask.getNodeName());
            params.put("sourceUserName", "");
            params.put("targetUserName", "");
            params.put("transferReason", transferReason);
            params.put("detailUrl", "");
            params.put("taskId", approvalTask.getId());
            params.put("instanceId", instance.getId());
            sendDTO.setParams(params);

            notifyService.sendNotify(sendDTO);
        } catch (Exception e) {
            log.warn("发送转审通知失败, taskId={}, error={}", approvalTask.getId(), e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transferExistingTasksForDelegation(Long delegationId) {
        WfDelegation delegation = delegationService.getById(delegationId);
        if (delegation == null) {
            log.warn("委托不存在, delegationId={}", delegationId);
            return;
        }
        if (!DelegationStatusEnum.ACTIVE.getCode().equals(delegation.getDelegationStatus())) {
            log.warn("委托未生效, delegationId={}, status={}", delegationId, delegation.getDelegationStatus());
            return;
        }

        LoginUserDTO originUser = SecurityUtils.getCurrentUserOpt().orElse(null);
        try {
            LoginUserDTO delegatorUser = LoginUserDTO.builder()
                    .userId(delegation.getDelegatorId())
                    .realName(delegation.getDelegatorName())
                    .tenantId(delegation.getTenantId())
                    .build();
            SecurityUtils.setCurrentUser(delegatorUser);

            List<WfApprovalTask> todoTasks = approvalTaskService.listTodoByUserId(delegation.getDelegatorId());
            if (todoTasks == null || todoTasks.isEmpty()) {
                log.info("委托人没有待办任务, delegatorId={}", delegation.getDelegatorId());
                return;
            }

            String processKeys = delegation.getProcessKeys();
            List<String> processKeyList = StrUtil.isNotBlank(processKeys)
                    ? Arrays.asList(processKeys.split(","))
                    : null;

            int successCount = 0;
            for (WfApprovalTask todoTask : todoTasks) {
                try {
                    if (processKeyList != null && !processKeyList.contains(todoTask.getProcessKey())) {
                        continue;
                    }

                    Task flowableTask = taskService.createTaskQuery()
                            .taskId(todoTask.getFlowableTaskId())
                            .singleResult();
                    if (flowableTask == null) {
                        continue;
                    }

                    handleDelegationForTask(todoTask, flowableTask);
                    successCount++;
                } catch (Exception e) {
                    log.error("委托转移任务失败, taskId={}, error={}", todoTask.getId(), e.getMessage(), e);
                }
            }

            log.info("委托生效转移现有待办完成, delegationId={}, 共{}个任务, 成功{}个",
                    delegationId, todoTasks.size(), successCount);
        } finally {
            if (originUser != null) {
                SecurityUtils.setCurrentUser(originUser);
            } else {
                SecurityUtils.clearCurrentUser();
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transferBackTasksForDelegation(Long delegationId) {
        WfDelegation delegation = delegationService.getById(delegationId);
        if (delegation == null) {
            log.warn("委托不存在, delegationId={}", delegationId);
            return;
        }

        List<WfTransferRecord> transferRecords = transferRecordService.listByDelegationId(delegationId);
        if (transferRecords == null || transferRecords.isEmpty()) {
            log.info("该委托未转移过任务, delegationId={}", delegationId);
            return;
        }

        LoginUserDTO originUser = SecurityUtils.getCurrentUserOpt().orElse(null);
        try {
            LoginUserDTO delegateeUser = LoginUserDTO.builder()
                    .userId(delegation.getDelegateeId())
                    .realName(delegation.getDelegateeName())
                    .tenantId(delegation.getTenantId())
                    .build();
            SecurityUtils.setCurrentUser(delegateeUser);

            String reason = "委托到期/撤销，任务转回原处理人";
            int successCount = 0;
            for (WfTransferRecord record : transferRecords) {
                try {
                    WfApprovalTask delegateeTask = approvalTaskService.getById(record.getTaskId());
                    if (delegateeTask == null) {
                        continue;
                    }
                    if (!TaskStatusEnum.PENDING.getCode().equals(delegateeTask.getTaskStatus())) {
                        continue;
                    }
                    if (!delegation.getDelegateeId().equals(delegateeTask.getAssigneeId())) {
                        continue;
                    }

                    WfTransferDTO transferDTO = new WfTransferDTO();
                    transferDTO.setTaskId(delegateeTask.getFlowableTaskId());
                    transferDTO.setTargetUserId(delegation.getDelegatorId());
                    transferDTO.setTargetUserName(delegation.getDelegatorName());
                    transferDTO.setActionRemark(reason);
                    transfer(transferDTO);

                    successCount++;
                } catch (Exception e) {
                    log.error("委托到期转回任务失败, recordId={}, error={}", record.getId(), e.getMessage(), e);
                }
            }

            log.info("委托到期/撤销转回任务完成, delegationId={}, 共{}条记录, 成功{}个",
                    delegationId, transferRecords.size(), successCount);
        } finally {
            if (originUser != null) {
                SecurityUtils.setCurrentUser(originUser);
            } else {
                SecurityUtils.clearCurrentUser();
            }
        }
    }

    private void handleParallelGatewayRejection(WfProcessInstance instance, Task currentTask,
                                                Long userId, WfApprovalActionDTO dto) {
        if (instance == null) {
            return;
        }

        ProcessDefinition processDef = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(instance.getFlowableProcessDefId())
                .singleResult();
        if (processDef == null) {
            return;
        }

        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDef.getId());
        Process process = bpmnModel.getProcesses().get(0);

        ParallelGateway parallelGateway = findAncestorParallelGateway(
                process, currentTask.getTaskDefinitionKey());
        if (parallelGateway == null) {
            return;
        }

        WfNodeConfig nodeConfig = nodeConfigService.getByNodeId(
                instance.getProcessVersionId(), parallelGateway.getId());
        if (nodeConfig == null || nodeConfig.getParallelRejectStrategy() == null) {
            return;
        }

        Integer rejectStrategy = nodeConfig.getParallelRejectStrategy();
        log.info("并行网关驳回策略, gatewayId={}, strategy={}", parallelGateway.getId(), rejectStrategy);

        if (ParallelGatewayRejectStrategyEnum.TERMINATE_OTHERS.getCode().equals(rejectStrategy)) {
            terminateOtherParallelBranches(instance, currentTask, parallelGateway, userId, dto);
        } else if (ParallelGatewayRejectStrategyEnum.WAIT_ALL_COMPLETE.getCode().equals(rejectStrategy)) {
            markParallelGatewayAsRejected(instance, parallelGateway, userId, dto);
        }
    }

    private ParallelGateway findAncestorParallelGateway(Process process, String currentNodeId) {
        FlowElement currentElement = process.getFlowElement(currentNodeId);
        if (!(currentElement instanceof FlowNode)) {
            return null;
        }

        Set<String> visited = new HashSet<>();
        Queue<FlowNode> queue = new LinkedList<>();
        queue.add((FlowNode) currentElement);

        while (!queue.isEmpty()) {
            FlowNode node = queue.poll();
            if (visited.contains(node.getId())) {
                continue;
            }
            visited.add(node.getId());

            List<SequenceFlow> incomingFlows = node.getIncomingFlows();
            if (incomingFlows == null || incomingFlows.isEmpty()) {
                continue;
            }

            for (SequenceFlow incomingFlow : incomingFlows) {
                FlowElement sourceElement = incomingFlow.getSourceRef();
                if (sourceElement instanceof ParallelGateway) {
                    return (ParallelGateway) sourceElement;
                }
                if (sourceElement instanceof FlowNode && !visited.contains(sourceElement.getId())) {
                    queue.add((FlowNode) sourceElement);
                }
            }
        }

        return null;
    }

    private void terminateOtherParallelBranches(WfProcessInstance instance, Task currentTask,
                                                ParallelGateway parallelGateway, Long userId,
                                                WfApprovalActionDTO dto) {
        String flowableInstId = instance.getFlowableProcessInstId();

        List<Task> allActiveTasks = taskService.createTaskQuery()
                .processInstanceId(flowableInstId)
                .list();

        Set<String> taskDefKeysToTerminate = new HashSet<>();
        Set<String> currentBranchNodeIds = new HashSet<>();
        for (SequenceFlow outgoingFlow : parallelGateway.getOutgoingFlows()) {
            List<String> branchNodeIds = collectBranchNodes(outgoingFlow.getTargetRef());
            if (branchNodeIds.contains(currentTask.getTaskDefinitionKey())) {
                currentBranchNodeIds.addAll(branchNodeIds);
            } else {
                taskDefKeysToTerminate.addAll(branchNodeIds);
            }
        }

        org.flowable.engine.runtime.Execution currentExecution = runtimeService.createExecutionQuery()
                .executionId(currentTask.getExecutionId())
                .singleResult();

        String scopeExecutionId = null;
        if (currentExecution != null && currentExecution.getParentId() != null) {
            scopeExecutionId = currentExecution.getParentId();
        }

        List<org.flowable.engine.runtime.Execution> allExecutions = runtimeService.createExecutionQuery()
                .processInstanceId(flowableInstId)
                .list();

        for (Task task : allActiveTasks) {
            if (task.getId().equals(currentTask.getId())) {
                continue;
            }
            if (taskDefKeysToTerminate.contains(task.getTaskDefinitionKey())) {
                log.info("终止并行分支任务, taskId={}, taskKey={}", task.getId(), task.getTaskDefinitionKey());

                taskService.addComment(task.getId(), flowableInstId, "TERMINATE",
                        "并行分支因其他分支驳回而终止");

                WfApprovalTask approvalTask = approvalTaskService.getByFlowableTaskId(task.getId());
                if (approvalTask != null) {
                    updateApprovalTask(approvalTask, TaskActionEnum.TERMINATE.getCode(),
                            "并行分支因其他分支驳回而终止", userId, null, null);

                    saveApprovalHistory(instance.getId(), null, task.getTaskDefinitionKey(),
                            task.getName(), HistoryActivityTypeEnum.TERMINATE.getCode(),
                            userId, SecurityUtils.getCurrentRealName(),
                            null, null, null, null,
                            "并行分支因其他分支驳回而终止", null, null,
                            ChronoUnit.MILLIS.between(approvalTask.getAssignTime(), LocalDateTime.now()),
                            LocalDateTime.now());
                }

                taskService.deleteTask(task.getId(), "并行分支终止: " +
                        (dto.getActionRemark() != null ? dto.getActionRemark() : "其他分支驳回"), true);
            }
        }

        for (org.flowable.engine.runtime.Execution execution : allExecutions) {
            if (execution.getId().equals(currentTask.getExecutionId())) {
                continue;
            }
            if (execution.getParentId() != null && execution.getParentId().equals(scopeExecutionId)) {
                if (execution.getIsActive() && !execution.getId().equals(currentTask.getExecutionId())) {
                    try {
                        runtimeService.deleteExecution(execution.getId(), true);
                        log.info("已删除并行分支执行流, executionId={}", execution.getId());
                    } catch (Exception e) {
                        log.warn("删除并行分支执行流失败, executionId={}, error={}",
                                execution.getId(), e.getMessage());
                    }
                }
            }
        }
    }

    private List<ParallelGateway> findParallelGatewayPair(BpmnModel bpmnModel, ParallelGateway forkGateway) {
        List<ParallelGateway> result = new ArrayList<>();
        result.add(forkGateway);

        Process process = bpmnModel.getProcesses().get(0);
        Collection<FlowElement> flowElements = process.getFlowElements();

        for (SequenceFlow outgoingFlow : forkGateway.getOutgoingFlows()) {
            FlowElement target = outgoingFlow.getTargetRef();
            if (target instanceof ParallelGateway) {
                result.add((ParallelGateway) target);
                return result;
            }

            List<String> branchNodes = collectBranchNodes(target);
            String lastNodeId = branchNodes.get(branchNodes.size() - 1);
            FlowElement lastElement = process.getFlowElement(lastNodeId);
            if (lastElement instanceof FlowNode) {
                FlowNode lastNode = (FlowNode) lastElement;
                for (SequenceFlow outFlow : lastNode.getOutgoingFlows()) {
                    if (outFlow.getTargetRef() instanceof ParallelGateway) {
                        result.add((ParallelGateway) outFlow.getTargetRef());
                        return result;
                    }
                }
            }
        }

        return result;
    }

    private void markParallelGatewayAsRejected(WfProcessInstance instance, ParallelGateway parallelGateway,
                                               Long userId, WfApprovalActionDTO dto) {
        String flowableInstId = instance.getFlowableProcessInstId();
        String gatewayId = parallelGateway.getId();

        String rejectVarName = "parallelReject_" + gatewayId;
        runtimeService.setVariable(flowableInstId, rejectVarName, true);
        runtimeService.setVariable(flowableInstId, rejectVarName + "_userId", userId);
        runtimeService.setVariable(flowableInstId, rejectVarName + "_remark",
                dto.getActionRemark() != null ? dto.getActionRemark() : "");
        runtimeService.setVariable(flowableInstId, rejectVarName + "_time",
                new Date());

        log.info("标记并行网关为驳回状态, gatewayId={}, instanceId={}", gatewayId, flowableInstId);
    }

    private List<WfParallelProgressVO> calculateParallelProgress(
            BpmnModel bpmnModel, WfProcessInstance instance,
            List<HistoricActivityInstance> historicActivities, List<Task> currentTasks) {

        List<WfParallelProgressVO> result = new ArrayList<>();

        Process process = bpmnModel.getProcesses().get(0);
        Collection<FlowElement> flowElements = process.getFlowElements();

        Set<String> completedNodeIds = historicActivities.stream()
                .filter(h -> h.getEndTime() != null)
                .map(HistoricActivityInstance::getActivityId)
                .collect(Collectors.toSet());

        Set<String> activeNodeIds = currentTasks.stream()
                .map(Task::getTaskDefinitionKey)
                .collect(Collectors.toSet());

        Map<String, String> nodeIdToExecutionId = new HashMap<>();
        for (Task task : currentTasks) {
            nodeIdToExecutionId.put(task.getTaskDefinitionKey(), task.getExecutionId());
        }

        for (FlowElement flowElement : flowElements) {
            if (!(flowElement instanceof ParallelGateway)) {
                continue;
            }

            ParallelGateway forkGateway = (ParallelGateway) flowElement;
            List<SequenceFlow> outgoingFlows = forkGateway.getOutgoingFlows();
            if (outgoingFlows.size() < 2) {
                continue;
            }

            WfParallelProgressVO progressVO = new WfParallelProgressVO();
            progressVO.setParallelGatewayId(forkGateway.getId());
            progressVO.setParallelGatewayName(forkGateway.getName() != null ? forkGateway.getName() : "并行分支");
            progressVO.setTotalBranches(outgoingFlows.size());

            WfNodeConfig nodeConfig = nodeConfigService.getByNodeId(
                    instance.getProcessVersionId(), forkGateway.getId());
            if (nodeConfig != null && nodeConfig.getParallelRejectStrategy() != null) {
                progressVO.setRejectStrategy(nodeConfig.getParallelRejectStrategy());
                ParallelGatewayRejectStrategyEnum strategyEnum =
                        ParallelGatewayRejectStrategyEnum.getByCode(nodeConfig.getParallelRejectStrategy());
                if (strategyEnum != null) {
                    progressVO.setRejectStrategyName(strategyEnum.getDesc());
                }
            }

            List<WfParallelBranchVO> branches = new ArrayList<>();
            int completedCount = 0;
            int activeCount = 0;
            boolean hasRejection = false;

            for (SequenceFlow flow : outgoingFlows) {
                FlowElement targetElement = flow.getTargetRef();
                WfParallelBranchVO branchVO = buildBranchVO(
                        targetElement, flow, instance, completedNodeIds, activeNodeIds, historicActivities);
                branches.add(branchVO);

                if ("COMPLETED".equals(branchVO.getStatus())) {
                    completedCount++;
                } else if ("ACTIVE".equals(branchVO.getStatus())) {
                    activeCount++;
                }

                if (Boolean.TRUE.equals(branchVO.getIsRejected())) {
                    hasRejection = true;
                }
            }

            progressVO.setCompletedBranches(completedCount);
            progressVO.setActiveBranches(activeCount);
            progressVO.setBranches(branches);
            progressVO.setIsRejected(hasRejection);
            progressVO.setProgressText(completedCount + "/" + outgoingFlows.size());

            result.add(progressVO);
        }

        return result;
    }

    private WfParallelBranchVO buildBranchVO(FlowElement firstNode, SequenceFlow flow,
                                             WfProcessInstance instance,
                                             Set<String> completedNodeIds,
                                             Set<String> activeNodeIds,
                                             List<HistoricActivityInstance> historicActivities) {
        WfParallelBranchVO branchVO = new WfParallelBranchVO();
        branchVO.setBranchId(flow.getId());
        branchVO.setBranchName(flow.getName() != null ? flow.getName() :
                (firstNode.getName() != null ? firstNode.getName() : firstNode.getId()));

        List<String> branchNodeIds = collectBranchNodes(firstNode);

        boolean branchCompleted = false;
        boolean branchActive = false;
        LocalDateTime branchStartTime = null;
        LocalDateTime branchEndTime = null;
        Long currentApproverId = null;
        String currentApproverName = null;
        boolean isRejected = false;

        for (int i = 0; i < branchNodeIds.size(); i++) {
            String nodeId = branchNodeIds.get(i);
            boolean nodeCompleted = completedNodeIds.contains(nodeId);
            boolean nodeActive = activeNodeIds.contains(nodeId);

            HistoricActivityInstance activity = historicActivities.stream()
                    .filter(h -> nodeId.equals(h.getActivityId()))
                    .findFirst()
                    .orElse(null);

            if (activity != null && i == 0) {
                branchStartTime = activity.getStartTime() != null ?
                        activity.getStartTime().toInstant().atZone(
                                java.time.ZoneId.systemDefault()).toLocalDateTime() : null;
            }

            if (activity != null && activity.getDeleteReason() != null
                    && activity.getDeleteReason().contains("驳")) {
                isRejected = true;
                branchEndTime = activity.getEndTime() != null ?
                        activity.getEndTime().toInstant().atZone(
                                java.time.ZoneId.systemDefault()).toLocalDateTime() : null;
            }

            if (nodeActive) {
                branchActive = true;
                WfApprovalTask approvalTask = approvalTaskService.getByInstanceIdAndNodeId(
                        instance.getId(), nodeId);
                if (approvalTask != null) {
                    currentApproverId = approvalTask.getAssigneeId();
                    if (approvalTask.getAssigneeId() != null) {
                        com.innerworkflow.auth.entity.SysUser user =
                                com.innerworkflow.common.util.SpringContextHolder
                                        .getBean(com.innerworkflow.auth.service.SysUserService.class)
                                        .getById(approvalTask.getAssigneeId());
                        if (user != null) {
                            currentApproverName = user.getRealName();
                        }
                    }
                }
                break;
            }

            if (!nodeCompleted && !nodeActive) {
                break;
            }

            if (nodeCompleted && i == branchNodeIds.size() - 1) {
                branchCompleted = true;
                branchEndTime = activity != null && activity.getEndTime() != null ?
                        activity.getEndTime().toInstant().atZone(
                                java.time.ZoneId.systemDefault()).toLocalDateTime() : null;
            }
        }

        List<String> completedNodes = branchNodeIds.stream()
                .filter(completedNodeIds::contains)
                .collect(Collectors.toList());
        List<String> pendingNodes = branchNodeIds.stream()
                .filter(n -> !completedNodeIds.contains(n))
                .collect(Collectors.toList());

        branchVO.setCompletedNodeIds(completedNodes);
        branchVO.setPendingNodeIds(pendingNodes);
        branchVO.setStartTime(branchStartTime);
        branchVO.setEndTime(branchEndTime);
        branchVO.setCurrentApproverId(currentApproverId);
        branchVO.setCurrentApproverName(currentApproverName);
        branchVO.setIsRejected(isRejected);

        if (isRejected) {
            branchVO.setStatus("REJECTED");
            branchVO.setStatusName("已驳回");
        } else if (branchCompleted) {
            branchVO.setStatus("COMPLETED");
            branchVO.setStatusName("已完成");
        } else if (branchActive) {
            branchVO.setStatus("ACTIVE");
            branchVO.setStatusName("审批中");
        } else {
            branchVO.setStatus("PENDING");
            branchVO.setStatusName("待执行");
        }

        return branchVO;
    }

    private List<String> collectBranchNodes(FlowElement startElement) {
        List<String> nodeIds = new ArrayList<>();
        FlowElement current = startElement;
        Set<String> visited = new HashSet<>();

        while (current != null && !visited.contains(current.getId())) {
            visited.add(current.getId());

            if (current instanceof UserTask || current instanceof ServiceTask
                    || current instanceof ScriptTask || current instanceof CallActivity) {
                nodeIds.add(current.getId());
            }

            if (current instanceof ParallelGateway) {
                break;
            }

            FlowNode flowNode = (FlowNode) current;
            if (flowNode.getOutgoingFlows() == null || flowNode.getOutgoingFlows().isEmpty()) {
                break;
            }

            SequenceFlow outgoingFlow = flowNode.getOutgoingFlows().get(0);
            current = outgoingFlow.getTargetRef();
        }

        return nodeIds;
    }

    private List<WfMultiInstanceSignVO> buildMultiInstanceSignList(WfProcessInstance instance) {
        List<WfMultiInstanceSignVO> result = new ArrayList<>();

        List<WfApprovalTask> allTasks = approvalTaskService.listByInstanceId(instance.getId());
        if (allTasks == null || allTasks.isEmpty()) {
            return result;
        }

        Map<String, List<WfApprovalTask>> tasksByNode = allTasks.stream()
                .filter(t -> t.getNodeId() != null)
                .collect(Collectors.groupingBy(WfApprovalTask::getNodeId));

        for (Map.Entry<String, List<WfApprovalTask>> entry : tasksByNode.entrySet()) {
            String nodeId = entry.getKey();
            List<WfApprovalTask> nodeTasks = entry.getValue();

            boolean hasMultiInstance = nodeTasks.stream()
                    .anyMatch(t -> t.getMultiInstanceFlag() != null && t.getMultiInstanceFlag() == 1);

            if (!hasMultiInstance && nodeTasks.size() <= 1) {
                continue;
            }

            WfNodeConfig nodeConfig = nodeConfigService.getByNodeId(instance.getProcessVersionId(), nodeId);
            if (nodeConfig == null || (nodeConfig.getMultiInstance() == null || nodeConfig.getMultiInstance() != 1)) {
                if (nodeTasks.size() <= 1) {
                    continue;
                }
            }

            WfMultiInstanceSignVO signVO = buildMultiInstanceSignVO(nodeId, nodeTasks, nodeConfig);
            if (signVO != null) {
                result.add(signVO);
            }
        }

        return result;
    }

    private WfMultiInstanceSignVO buildMultiInstanceSignVO(String nodeId,
                                                           List<WfApprovalTask> nodeTasks,
                                                           WfNodeConfig nodeConfig) {
        WfMultiInstanceSignVO signVO = new WfMultiInstanceSignVO();
        signVO.setNodeId(nodeId);

        WfApprovalTask sampleTask = nodeTasks.get(0);
        signVO.setNodeName(sampleTask.getNodeName());

        if (nodeConfig != null) {
            signVO.setApproveType(nodeConfig.getApproveType());
            ApproveTypeEnum approveTypeEnum = ApproveTypeEnum.getByCode(nodeConfig.getApproveType());
            if (approveTypeEnum != null) {
                signVO.setApproveTypeName(approveTypeEnum.getDesc());
            }

            signVO.setCompletionType(nodeConfig.getMultiInstanceCompletionType());
            MultiInstanceCompletionTypeEnum completionTypeEnum =
                    MultiInstanceCompletionTypeEnum.getByCode(nodeConfig.getMultiInstanceCompletionType());
            if (completionTypeEnum != null) {
                signVO.setCompletionTypeName(completionTypeEnum.getDesc());
            }

            signVO.setPassPercentage(nodeConfig.getPassPercentage());
            signVO.setVetoEnabled(nodeConfig.getVetoEnabled() != null && nodeConfig.getVetoEnabled() == 1);
        } else if (sampleTask.getApproveType() != null) {
            signVO.setApproveType(sampleTask.getApproveType());
            ApproveTypeEnum approveTypeEnum = ApproveTypeEnum.getByCode(sampleTask.getApproveType());
            if (approveTypeEnum != null) {
                signVO.setApproveTypeName(approveTypeEnum.getDesc());
            }
        }

        List<WfMultiInstanceSignVO.SignerStatusVO> signers = new ArrayList<>();
        int approvedCount = 0;
        int rejectedCount = 0;
        int pendingCount = 0;

        for (WfApprovalTask task : nodeTasks) {
            WfMultiInstanceSignVO.SignerStatusVO signer = buildSignerStatusVO(task);
            signers.add(signer);

            if (MultiInstanceSignStatusEnum.APPROVED.getCode().equals(signer.getSignStatus())) {
                approvedCount++;
            } else if (MultiInstanceSignStatusEnum.REJECTED.getCode().equals(signer.getSignStatus())) {
                rejectedCount++;
            } else {
                pendingCount++;
            }
        }

        signVO.setSigners(signers);
        signVO.setTotalSigners(signers.size());
        signVO.setApprovedCount(approvedCount);
        signVO.setRejectedCount(rejectedCount);
        signVO.setPendingCount(pendingCount);
        signVO.setProgressText(approvedCount + "/" + signers.size() + " 已通过");

        return signVO;
    }

    private WfMultiInstanceSignVO.SignerStatusVO buildSignerStatusVO(WfApprovalTask task) {
        WfMultiInstanceSignVO.SignerStatusVO signer = new WfMultiInstanceSignVO.SignerStatusVO();
        signer.setUserId(task.getAssigneeId());
        signer.setAssignTime(task.getAssignTime());
        signer.setHandleTime(task.getActionTime());
        signer.setDuration(task.getActionDuration());
        signer.setComment(task.getActionRemark());
        signer.setSignatureUrl(task.getSignatureUrl());
        signer.setAttachmentIds(task.getAttachmentIds());

        if (task.getAssigneeId() != null) {
            SysUser user = sysUserService.getById(task.getAssigneeId());
            if (user != null) {
                signer.setUserName(user.getRealName());
                signer.setUserAvatar(user.getAvatar());
                if (user.getDeptId() != null) {
                    signer.setDeptName(user.getDeptId().toString());
                }
            }
        }

        Integer taskStatus = task.getTaskStatus();
        Integer action = task.getAction();

        if (TaskStatusEnum.PENDING.getCode().equals(taskStatus)) {
            signer.setSignStatus(MultiInstanceSignStatusEnum.PENDING.getCode());
            signer.setSignStatusName(MultiInstanceSignStatusEnum.PENDING.getDesc());
        } else if (TaskActionEnum.AGREE.getCode().equals(action)) {
            signer.setSignStatus(MultiInstanceSignStatusEnum.APPROVED.getCode());
            signer.setSignStatusName(MultiInstanceSignStatusEnum.APPROVED.getDesc());
        } else if (TaskActionEnum.REJECT.getCode().equals(action)) {
            signer.setSignStatus(MultiInstanceSignStatusEnum.REJECTED.getCode());
            signer.setSignStatusName(MultiInstanceSignStatusEnum.REJECTED.getDesc());
        } else {
            signer.setSignStatus(MultiInstanceSignStatusEnum.PENDING.getCode());
            signer.setSignStatusName(MultiInstanceSignStatusEnum.PENDING.getDesc());
        }

        return signer;
    }

    private void updateMultiInstanceSignCounters(Task task, boolean isApprove) {
        try {
            String processInstanceId = task.getProcessInstanceId();
            String taskDefinitionKey = task.getTaskDefinitionKey();
            String executionId = task.getExecutionId();

            WfApprovalTask approvalTask = approvalTaskService.getByFlowableTaskId(task.getId());
            if (approvalTask == null || approvalTask.getInstanceId() == null) {
                return;
            }

            WfProcessInstance instance = processInstanceService.getById(approvalTask.getInstanceId());
            if (instance == null || instance.getProcessVersionId() == null) {
                return;
            }

            WfNodeConfig nodeConfig = nodeConfigService.getByNodeId(instance.getProcessVersionId(), taskDefinitionKey);
            if (nodeConfig == null) {
                return;
            }

            boolean isMultiInstance = (nodeConfig.getMultiInstance() != null && nodeConfig.getMultiInstance() == 1)
                    || (nodeConfig.getApproveType() != null
                    && (nodeConfig.getApproveType().equals(ApproveTypeEnum.ALL_SIGN.getCode())
                    || nodeConfig.getApproveType().equals(ApproveTypeEnum.OR_SIGN.getCode())));

            if (!isMultiInstance) {
                return;
            }

            String rootExecutionId = findMultiInstanceRootExecutionId(processInstanceId, executionId, taskDefinitionKey);
            if (rootExecutionId == null) {
                rootExecutionId = processInstanceId;
            }

            Integer currentApprove = getExecutionVariableAsInt(rootExecutionId, "signApproveCount");
            Integer currentReject = getExecutionVariableAsInt(rootExecutionId, "signRejectCount");

            if (isApprove) {
                currentApprove = (currentApprove == null ? 0 : currentApprove) + 1;
                runtimeService.setVariable(rootExecutionId, "signApproveCount", currentApprove);
                runtimeService.setVariableLocal(rootExecutionId, "signApproveCount", currentApprove);
                log.info("多实例同意计数更新, nodeId={}, rootExecutionId={}, approveCount={}",
                        taskDefinitionKey, rootExecutionId, currentApprove);
            } else {
                currentReject = (currentReject == null ? 0 : currentReject) + 1;
                runtimeService.setVariable(rootExecutionId, "signRejectCount", currentReject);
                runtimeService.setVariableLocal(rootExecutionId, "signRejectCount", currentReject);
                log.info("多实例拒绝计数更新, nodeId={}, rootExecutionId={}, rejectCount={}",
                        taskDefinitionKey, rootExecutionId, currentReject);
            }

        } catch (Exception e) {
            log.error("更新多实例会签计数失败, taskId={}, isApprove={}, error={}",
                    task.getId(), isApprove, e.getMessage(), e);
        }
    }

    private String findMultiInstanceRootExecutionId(String processInstanceId, String executionId, String taskDefinitionKey) {
        try {
            List<Execution> executions = runtimeService.createExecutionQuery()
                    .processInstanceId(processInstanceId)
                    .activityId(taskDefinitionKey)
                    .list();
            if (executions != null && !executions.isEmpty()) {
                Execution parentExecution = runtimeService.createExecutionQuery()
                        .executionId(executions.get(0).getParentId())
                        .singleResult();
                if (parentExecution != null) {
                    return parentExecution.getId();
                }
            }
            Execution current = runtimeService.createExecutionQuery()
                    .executionId(executionId)
                    .singleResult();
            if (current != null && current.getParentId() != null) {
                return current.getParentId();
            }
            return processInstanceId;
        } catch (Exception e) {
            log.warn("查找多实例根执行流失败, executionId={}, error={}", executionId, e.getMessage());
            return processInstanceId;
        }
    }

    private Integer getExecutionVariableAsInt(String executionId, String key) {
        try {
            Object val = runtimeService.getVariable(executionId, key);
            if (val == null) {
                return null;
            }
            if (val instanceof Number) {
                return ((Number) val).intValue();
            }
            if (val instanceof String) {
                try {
                    return Integer.parseInt((String) val);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private WfTrackingMapVO buildTrackingMap(WfProcessInstance instance,
                                             List<HistoricActivityInstance> historicActivities,
                                             BpmnModel bpmnModel) {
        WfTrackingMapVO trackingMap = new WfTrackingMapVO();
        trackingMap.setInstanceId(instance.getId());
        trackingMap.setInstanceNo(instance.getInstanceNo());
        trackingMap.setTitle(instance.getTitle());

        if (historicActivities == null || historicActivities.isEmpty()) {
            trackingMap.setNodes(new ArrayList<>());
            trackingMap.setEdges(new ArrayList<>());
            return trackingMap;
        }

        String processKey = instance.getProcessKey();

        Map<String, List<HistoricActivityInstance>> activitiesByNode = historicActivities.stream()
                .filter(a -> !"sequenceFlow".equals(a.getActivityType()))
                .collect(Collectors.groupingBy(HistoricActivityInstance::getActivityId, LinkedHashMap::new, Collectors.toList()));

        Set<String> actualFlowIds = historicActivities.stream()
                .filter(a -> "sequenceFlow".equals(a.getActivityType()))
                .map(HistoricActivityInstance::getActivityId)
                .collect(Collectors.toSet());

        List<WfApprovalHistory> currentHistory = approvalHistoryService.listValidByInstanceId(instance.getId());
        Map<String, List<WfApprovalHistory>> historyByNode = currentHistory.stream()
                .filter(h -> h.getNodeId() != null)
                .collect(Collectors.groupingBy(WfApprovalHistory::getNodeId));

        Map<String, Double> historicalAvgByNode = calculateHistoricalAvgDuration(processKey, activitiesByNode.keySet());

        long historicalInstanceCount = countHistoricalInstances(processKey);
        trackingMap.setHistoricalInstanceCount(historicalInstanceCount);

        List<WfTrackingMapVO.TrackingNodeVO> nodes = new ArrayList<>();
        List<WfTrackingMapVO.TrackingEdgeVO> edges = new ArrayList<>();

        for (Map.Entry<String, List<HistoricActivityInstance>> entry : activitiesByNode.entrySet()) {
            String nodeId = entry.getKey();
            List<HistoricActivityInstance> nodeActivities = entry.getValue();

            HistoricActivityInstance firstActivity = nodeActivities.get(0);
            WfTrackingMapVO.TrackingNodeVO nodeVO = new WfTrackingMapVO.TrackingNodeVO();
            nodeVO.setNodeId(nodeId);
            nodeVO.setNodeName(firstActivity.getActivityName());
            nodeVO.setNodeType(firstActivity.getActivityType());

            String activityType = firstActivity.getActivityType();
            if ("userTask".equals(activityType) || "multiInstanceBody".equals(activityType)) {
                nodeVO.setNodeCategory(1);
            } else if ("startEvent".equals(activityType)) {
                nodeVO.setNodeCategory(2);
            } else if ("endEvent".equals(activityType)) {
                nodeVO.setNodeCategory(3);
            } else if (activityType != null && activityType.contains("Gateway")) {
                nodeVO.setNodeCategory(4);
            } else if ("callActivity".equals(activityType)) {
                nodeVO.setNodeCategory(5);
            } else {
                nodeVO.setNodeCategory(0);
            }

            HistoricActivityInstance lastActivity = nodeActivities.get(nodeActivities.size() - 1);
            if (lastActivity.getEndTime() != null) {
                nodeVO.setStatus("completed");
                nodeVO.setStatusName("已完成");
                nodeVO.setEndTime(lastActivity.getEndTime());
            } else {
                nodeVO.setStatus("active");
                nodeVO.setStatusName("进行中");
            }

            if (firstActivity.getStartTime() != null) {
                nodeVO.setStartTime(firstActivity.getStartTime());
            }

            if (firstActivity.getStartTime() != null && lastActivity.getEndTime() != null) {
                long durationMs = java.time.Duration.between(firstActivity.getStartTime(), lastActivity.getEndTime()).toMillis();
                nodeVO.setDuration(durationMs);
            }

            Double histAvg = historicalAvgByNode.get(nodeId);
            nodeVO.setHistoricalAvgDuration(histAvg);

            if (nodeVO.getDuration() != null && histAvg != null && histAvg > 0) {
                double deviation = (nodeVO.getDuration() - histAvg) / histAvg;
                nodeVO.setDurationDeviation(deviation);
                nodeVO.setIsBottleneck(deviation > 0.5);
            } else {
                nodeVO.setIsBottleneck(false);
            }

            List<WfTrackingMapVO.NodeOperatorVO> operators = buildNodeOperators(nodeActivities, historyByNode.getOrDefault(nodeId, List.of()));
            nodeVO.setOperators(operators);

            List<WfApprovalHistory> nodeHistoryList = historyByNode.getOrDefault(nodeId, List.of());
            if (!nodeHistoryList.isEmpty()) {
                WfApprovalHistory lastHistory = nodeHistoryList.get(nodeHistoryList.size() - 1);
                nodeVO.setActionRemark(lastHistory.getActionRemark());
                nodeVO.setSignatureUrl(lastHistory.getSignatureUrl());
                if (lastHistory.getActivityType() != null) {
                    TaskActionEnum actionEnum = TaskActionEnum.getByCode(lastHistory.getActivityType());
                    if (actionEnum != null) {
                        nodeVO.setActionName(actionEnum.getDesc());
                    }
                }
            }

            nodes.add(nodeVO);
        }

        double overallAvg = nodes.stream()
                .filter(n -> n.getDuration() != null)
                .mapToLong(WfTrackingMapVO.TrackingNodeVO::getDuration)
                .average().orElse(0.0);
        trackingMap.setAverageDuration(overallAvg);

        if (bpmnModel != null && !bpmnModel.getProcesses().isEmpty()) {
            Process process = bpmnModel.getProcesses().get(0);
            for (FlowElement element : process.getFlowElements()) {
                if (element instanceof SequenceFlow sequenceFlow) {
                    if (actualFlowIds.contains(sequenceFlow.getId())) {
                        WfTrackingMapVO.TrackingEdgeVO edge = new WfTrackingMapVO.TrackingEdgeVO();
                        edge.setSourceId(sequenceFlow.getSourceRef());
                        edge.setTargetId(sequenceFlow.getTargetRef());
                        edge.setLabel(sequenceFlow.getName());
                        edges.add(edge);
                    }
                }
            }
        }

        trackingMap.setNodes(nodes);
        trackingMap.setEdges(edges);

        return trackingMap;
    }

    private Map<String, Double> calculateHistoricalAvgDuration(String processKey, Set<String> nodeIds) {
        Map<String, Double> result = new HashMap<>();
        if (processKey == null || nodeIds == null || nodeIds.isEmpty()) {
            return result;
        }

        try {
            List<WfProcessInstance> allInstances = processInstanceService.listByProcessKey(processKey);
            if (allInstances == null || allInstances.isEmpty()) {
                return result;
            }

            Map<String, List<Long>> durationByNode = new HashMap<>();

            for (WfProcessInstance inst : allInstances) {
                List<WfApprovalHistory> historyList = approvalHistoryService.listValidByInstanceId(inst.getId());
                for (WfApprovalHistory h : historyList) {
                    if (h.getNodeId() != null && nodeIds.contains(h.getNodeId()) && h.getDuration() != null && h.getDuration() > 0) {
                        durationByNode.computeIfAbsent(h.getNodeId(), k -> new ArrayList<>()).add(h.getDuration());
                    }
                }
            }

            for (Map.Entry<String, List<Long>> entry : durationByNode.entrySet()) {
                double avg = entry.getValue().stream().mapToLong(Long::longValue).average().orElse(0.0);
                result.put(entry.getKey(), avg);
            }
        } catch (Exception e) {
            log.warn("计算历史平均耗时失败, processKey={}, error={}", processKey, e.getMessage());
        }

        return result;
    }

    private long countHistoricalInstances(String processKey) {
        if (processKey == null) {
            return 0L;
        }
        try {
            List<WfProcessInstance> allInstances = processInstanceService.listByProcessKey(processKey);
            return allInstances != null ? allInstances.size() : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    private List<WfTrackingMapVO.NodeOperatorVO> buildNodeOperators(
            List<HistoricActivityInstance> nodeActivities,
            List<WfApprovalHistory> nodeHistoryList) {

        Map<Long, WfApprovalHistory> historyByOperator = nodeHistoryList.stream()
                .filter(h -> h.getOperatorId() != null)
                .collect(Collectors.toMap(WfApprovalHistory::getOperatorId, h -> h, (a, b) -> b));

        List<WfTrackingMapVO.NodeOperatorVO> operators = new ArrayList<>();

        for (HistoricActivityInstance activity : nodeActivities) {
            WfTrackingMapVO.NodeOperatorVO operator = new WfTrackingMapVO.NodeOperatorVO();
            if (activity.getAssignee() != null) {
                try {
                    Long userId = Long.parseLong(activity.getAssignee());
                    operator.setUserId(userId);

                    SysUser user = sysUserService.getById(userId);
                    if (user != null) {
                        operator.setUserName(user.getRealName());
                        operator.setUserAvatar(user.getAvatar());
                        operator.setDeptName(user.getDeptId() != null ? user.getDeptId().toString() : null);
                    }

                    WfApprovalHistory matchingHistory = historyByOperator.get(userId);
                    if (matchingHistory != null) {
                        if (matchingHistory.getActivityType() != null) {
                            TaskActionEnum actionEnum = TaskActionEnum.getByCode(matchingHistory.getActivityType());
                            if (actionEnum != null) {
                                operator.setAction(actionEnum.getCode().toString());
                                operator.setActionName(actionEnum.getDesc());
                            }
                        }
                        operator.setActionRemark(matchingHistory.getActionRemark());
                        operator.setOperateTime(matchingHistory.getOperateTime());
                        operator.setDuration(matchingHistory.getDuration());
                    } else {
                        operator.setOperateTime(activity.getStartTime());
                        if (activity.getEndTime() != null && activity.getStartTime() != null) {
                            operator.setDuration(java.time.Duration.between(
                                    activity.getStartTime(), activity.getEndTime()).toMillis());
                        }
                    }
                } catch (NumberFormatException ignored) {}
            } else {
                operator.setOperateTime(activity.getStartTime());
                if (activity.getEndTime() != null && activity.getStartTime() != null) {
                    operator.setDuration(java.time.Duration.between(
                            activity.getStartTime(), activity.getEndTime()).toMillis());
                }
            }
            operators.add(operator);
        }

        return operators;
    }
}
