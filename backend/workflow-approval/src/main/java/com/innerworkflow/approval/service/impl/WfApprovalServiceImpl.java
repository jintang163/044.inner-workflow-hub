package com.innerworkflow.approval.service.impl;

import com.innerworkflow.approval.dto.*;
import com.innerworkflow.approval.entity.*;
import com.innerworkflow.approval.service.*;
import com.innerworkflow.approval.vo.*;
import com.innerworkflow.bpmn.entity.WfNodeConfig;
import com.innerworkflow.bpmn.entity.WfProcessDefinition;
import com.innerworkflow.bpmn.entity.WfProcessVersion;
import com.innerworkflow.bpmn.service.WfNodeConfigService;
import com.innerworkflow.bpmn.service.WfProcessDefinitionService;
import com.innerworkflow.bpmn.service.WfProcessVersionService;
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
        instance.setStartDeptId(SecurityUtils.getCurrentDeptId());
        instance.setStartTime(LocalDateTime.now());
        instance.setPriority(dto.getPriority() != null ? dto.getPriority() : 0);
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
                dto.getTargetUserId(), null, null, null, dto.getActionRemark(), null,
                null, null, LocalDateTime.now());

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

        String targetNodeId = dto.getTargetNodeId();
        if (StrUtil.isBlank(targetNodeId)) {
            WfProcessInstance instance = processInstanceService.getById(approvalTask.getInstanceId());
            if (instance != null) {
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
        }

        if (StrUtil.isBlank(targetNodeId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "驳回目标节点不能为空");
        }

        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(task.getProcessInstanceId())
                .moveActivityIdTo(task.getTaskDefinitionKey(), targetNodeId)
                .changeState();

        updateApprovalTask(approvalTask, TaskActionEnum.SEND_BACK.getCode(), dto.getActionRemark(),
                userId, null, null);

        approvalHistoryService.markInvalidByInstanceIdAndNodeId(approvalTask.getInstanceId(), task.getTaskDefinitionKey());

        saveApprovalHistory(approvalTask.getInstanceId(), null, task.getTaskDefinitionKey(), task.getName(),
                HistoryActivityTypeEnum.REJECT.getCode(), userId, SecurityUtils.getCurrentRealName(),
                null, null, targetNodeId, null, dto.getActionRemark(), null,
                null, null, LocalDateTime.now());

        syncTasksFromFlowable(approvalTask.getInstanceId(), task.getProcessInstanceId());

        log.info("任务驳回, taskId={}, targetNodeId={}, userId={}", dto.getTaskId(), targetNodeId, userId);
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

        try {
            ProcessDefinition processDef = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionId(instance.getFlowableProcessDefId())
                    .singleResult();
            if (processDef != null) {
                BpmnModel bpmnModel = repositoryService.getBpmnModel(processDef.getId());
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

        try {
            ProcessDefinition processDef = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionId(instance.getFlowableProcessDefId())
                    .singleResult();
            if (processDef != null) {
                BpmnModel bpmnModel = repositoryService.getBpmnModel(processDef.getId());
                List<WfParallelProgressVO> parallelProgressList = calculateParallelProgress(
                        bpmnModel, instance, historicActivities, currentFlowableTasks);
                detailVO.setParallelProgressList(parallelProgressList);
            }
        } catch (Exception e) {
            log.error("计算并行进度失败, instanceId={}, error={}", instanceId, e.getMessage(), e);
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
        if (nodeConfig == null || nodeConfig.getRefuseStrategy() == null) {
            return;
        }

        Integer rejectStrategy = nodeConfig.getRefuseStrategy();
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

        List<ParallelGateway> gatewayPair = findParallelGatewayPair(
                repositoryService.getBpmnModel(instance.getFlowableProcessDefId()), parallelGateway);

        Set<String> taskDefKeysToTerminate = new HashSet<>();
        for (SequenceFlow outgoingFlow : parallelGateway.getOutgoingFlows()) {
            List<String> branchNodeIds = collectBranchNodes(outgoingFlow.getTargetRef());
            boolean isCurrentBranch = branchNodeIds.contains(currentTask.getTaskDefinitionKey());
            if (!isCurrentBranch) {
                taskDefKeysToTerminate.addAll(branchNodeIds);
            }
        }

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

                runtimeService.deleteProcessInstance(task.getExecutionId(),
                        "并行分支终止: " + (dto.getActionRemark() != null ? dto.getActionRemark() : "其他分支驳回"));
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
            if (nodeConfig != null && nodeConfig.getRefuseStrategy() != null) {
                progressVO.setRejectStrategy(nodeConfig.getRefuseStrategy());
                ParallelGatewayRejectStrategyEnum strategyEnum =
                        ParallelGatewayRejectStrategyEnum.getByCode(nodeConfig.getRefuseStrategy());
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
}
