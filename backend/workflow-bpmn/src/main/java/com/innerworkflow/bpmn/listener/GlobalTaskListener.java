package com.innerworkflow.bpmn.listener;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.innerworkflow.approval.entity.WfApprovalTask;
import com.innerworkflow.approval.entity.WfCcTask;
import com.innerworkflow.approval.entity.WfProcessInstance;
import com.innerworkflow.approval.enums.CcTypeEnum;
import com.innerworkflow.approval.handler.CcUserResolver;
import com.innerworkflow.approval.service.WfApprovalTaskService;
import com.innerworkflow.approval.service.WfCcTaskService;
import com.innerworkflow.approval.service.WfProcessInstanceService;
import com.innerworkflow.bpmn.entity.WfNodeConfig;
import com.innerworkflow.bpmn.entity.WfProcessVersion;
import com.innerworkflow.bpmn.handler.TaskAssigneeHandler;
import com.innerworkflow.bpmn.service.WfNodeConfigService;
import com.innerworkflow.bpmn.service.WfProcessVersionService;
import com.innerworkflow.common.config.FrontendConfig;
import com.innerworkflow.common.enums.TaskStatusEnum;
import com.innerworkflow.common.util.JsonUtils;
import com.innerworkflow.common.util.SpringContextHolder;
import com.innerworkflow.notify.dto.NotifySendDTO;
import com.innerworkflow.notify.enums.EventTypeEnum;
import com.innerworkflow.notify.service.WfNotifyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GlobalTaskListener implements FlowableEventListener {

    private final WfNodeConfigService nodeConfigService;
    private final WfProcessVersionService processVersionService;
    private final TaskAssigneeHandler taskAssigneeHandler;
    private final WfApprovalTaskService approvalTaskService;
    private final WfCcTaskService ccTaskService;
    private final WfProcessInstanceService processInstanceService;
    private final WfNotifyService notifyService;
    private final CcUserResolver ccUserResolver;
    private final FrontendConfig frontendConfig;
    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final org.flowable.engine.HistoryService historyService;

    @Override
    public void onEvent(FlowableEvent event) {
        if (!(event instanceof FlowableEngineEntityEvent)) {
            return;
        }

        FlowableEngineEntityEvent entityEvent = (FlowableEngineEntityEvent) event;
        Object entity = entityEvent.getEntity();

        if (!(entity instanceof Task)) {
            return;
        }

        Task task = (Task) entity;
        String eventType = event.getType().name();

        log.debug("全局任务监听器 - 事件: {}, 任务ID: {}, 任务名称: {}, 流程实例ID: {}",
                eventType, task.getId(), task.getName(), task.getProcessInstanceId());

        try {
            if (FlowableEngineEventType.TASK_CREATED.name().equals(eventType)) {
                handleTaskCreate(task);
            } else if (FlowableEngineEventType.TASK_ASSIGNED.name().equals(eventType)) {
                handleTaskAssignment(task);
            } else if (FlowableEngineEventType.TASK_COMPLETED.name().equals(eventType)) {
                handleTaskComplete(task);
            } else if (FlowableEngineEventType.TASK_DELETED.name().equals(eventType)) {
                handleTaskDelete(task);
            }
        } catch (Exception e) {
            log.error("全局任务监听器处理异常, eventType={}, taskId={}, error={}",
                    eventType, task.getId(), e.getMessage(), e);
        }
    }

    private void handleTaskCreate(Task task) {
        log.info("任务创建: taskId={}, taskName={}, processInstanceId={}",
                task.getId(), task.getName(), task.getProcessInstanceId());

        try {
            WfProcessInstance instance = processInstanceService.getByFlowableInstId(task.getProcessInstanceId());
            if (instance == null) {
                log.warn("未找到流程实例, flowableInstId={}", task.getProcessInstanceId());
                return;
            }

            Long processVersionId = instance.getProcessVersionId();
            String nodeId = task.getTaskDefinitionKey();

            WfNodeConfig nodeConfig = nodeConfigService.getByNodeId(processVersionId, nodeId);
            if (nodeConfig == null) {
                log.warn("未找到节点配置, processVersionId={}, nodeId={}", processVersionId, nodeId);
                createApprovalTaskWithDefaults(task, instance);
                sendTaskCreateNotify(task, instance, null);
                return;
            }

            if (StrUtil.isBlank(task.getAssignee())) {
                assignTaskAssignee(task, nodeConfig, instance);
            }

            WfApprovalTask existingTask = approvalTaskService.getByFlowableTaskId(task.getId());
            if (existingTask == null) {
                createApprovalTask(task, instance, nodeConfig);
            }

            sendTaskCreateNotify(task, instance, nodeConfig);

            createCcTasksFromConfig(instance, nodeConfig);

        } catch (Exception e) {
            log.error("任务创建处理失败, taskId={}, error={}", task.getId(), e.getMessage(), e);
        }
    }

    private void assignTaskAssignee(Task task, WfNodeConfig nodeConfig, WfProcessInstance instance) {
        try {
            Map<String, Object> variables = runtimeService.getVariables(task.getExecutionId());
            if (variables != null && !variables.isEmpty()) {
                for (Map.Entry<String, Object> entry : variables.entrySet()) {
                    task.setVariable(entry.getKey(), entry.getValue());
                }
            }
            Long startUserId = instance.getStartUserId();
            Long startDeptId = instance.getStartDeptId();
            task.setVariable("startUserId", startUserId);
            task.setVariable("startDeptId", startDeptId);

            com.innerworkflow.bpmn.handler.AssigneeResolver resolver = getAssigneeResolver(nodeConfig.getAssigneeType());
            if (resolver != null) {
                List<Long> assignees = resolver.resolve(nodeConfig, startUserId, startDeptId);
                if (assignees != null && !assignees.isEmpty()) {
                    if (assignees.size() == 1) {
                        taskService.setAssignee(task.getId(), String.valueOf(assignees.get(0)));
                    } else {
                        for (Long assignee : assignees) {
                            taskService.addCandidateUser(task.getId(), String.valueOf(assignee));
                        }
                    }
                }
            }

            if (StrUtil.isNotBlank(nodeConfig.getNodeName())) {
                task.setName(nodeConfig.getNodeName());
                taskService.saveTask(task);
            }
        } catch (Exception e) {
            log.error("分配任务审批人失败, taskId={}, error={}", task.getId(), e.getMessage(), e);
        }
    }

    private Map<Integer, com.innerworkflow.bpmn.handler.AssigneeResolver> resolverCache = new HashMap<>();

    private com.innerworkflow.bpmn.handler.AssigneeResolver getAssigneeResolver(Integer assigneeType) {
        if (resolverCache.isEmpty()) {
            try {
                Map<String, com.innerworkflow.bpmn.handler.AssigneeResolver> beans =
                        SpringContextHolder.getApplicationContext()
                                .getBeansOfType(com.innerworkflow.bpmn.handler.AssigneeResolver.class);
                for (com.innerworkflow.bpmn.handler.AssigneeResolver resolver : beans.values()) {
                    resolverCache.put(resolver.getAssigneeType(), resolver);
                }
            } catch (Exception e) {
                log.warn("获取审批人解析器失败: {}", e.getMessage());
            }
        }
        return resolverCache.get(assigneeType);
    }

    private void createApprovalTaskWithDefaults(Task task, WfProcessInstance instance) {
        try {
            WfApprovalTask approvalTask = buildApprovalTask(task, instance, null);
            approvalTaskService.save(approvalTask);
            log.info("创建默认待办任务成功, taskId={}, flowableTaskId={}", approvalTask.getId(), task.getId());
        } catch (Exception e) {
            log.error("创建默认待办任务失败, taskId={}, error={}", task.getId(), e.getMessage(), e);
        }
    }

    private void createApprovalTask(Task task, WfProcessInstance instance, WfNodeConfig nodeConfig) {
        try {
            WfApprovalTask approvalTask = buildApprovalTask(task, instance, nodeConfig);
            approvalTaskService.save(approvalTask);
            log.info("创建待办任务成功, taskId={}, flowableTaskId={}, assigneeId={}",
                    approvalTask.getId(), task.getId(), approvalTask.getAssigneeId());
        } catch (Exception e) {
            log.error("创建待办任务失败, taskId={}, error={}", task.getId(), e.getMessage(), e);
        }
    }

    private WfApprovalTask buildApprovalTask(Task task, WfProcessInstance instance, WfNodeConfig nodeConfig) {
        WfApprovalTask approvalTask = new WfApprovalTask();
        approvalTask.setTaskNo(generateTaskNo());
        approvalTask.setInstanceId(instance.getId());
        approvalTask.setFlowableTaskId(task.getId());
        approvalTask.setFlowableExecutionId(task.getExecutionId());
        approvalTask.setProcessKey(instance.getProcessKey());
        approvalTask.setNodeId(task.getTaskDefinitionKey());
        approvalTask.setNodeName(nodeConfig != null ? nodeConfig.getNodeName() : task.getName());
        approvalTask.setNodeType(1);
        approvalTask.setApproveType(nodeConfig != null ? nodeConfig.getApproveType() : 1);
        approvalTask.setMultiInstanceFlag(nodeConfig != null ? nodeConfig.getMultiInstance() : 0);
        approvalTask.setAssigneeId(parseAssigneeId(task.getAssignee()));
        approvalTask.setAssignTime(LocalDateTime.now());

        if (nodeConfig != null && nodeConfig.getTimeoutHours() != null && nodeConfig.getTimeoutHours() > 0) {
            approvalTask.setDueTime(LocalDateTime.now().plusHours(nodeConfig.getTimeoutHours()));
        }

        approvalTask.setTaskStatus(TaskStatusEnum.PENDING.getCode());
        approvalTask.setSourceType(1);
        approvalTask.setEscalateLevel(0);
        return approvalTask;
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

    private void createCcTasksFromConfig(WfProcessInstance instance, WfNodeConfig nodeConfig) {
        try {
            if (nodeConfig == null || nodeConfig.getNotifyConfig() == null) {
                return;
            }

            Map<String, Object> notifyConfig = nodeConfig.getNotifyConfig();
            Object nodeStartCcObj = notifyConfig.get("nodeStartCc");

            if (nodeStartCcObj != null) {
                List<Long> ccUserIds = ccUserResolver.resolveCcUsers(
                        nodeStartCcObj, instance.getStartUserId(), instance.getStartDeptId());

                if (ccUserIds != null && !ccUserIds.isEmpty()) {
                    List<WfCcTask> ccTasks = createAndSaveCcTasks(
                            instance, nodeConfig, ccUserIds, CcTypeEnum.AUTO_NODE_START);

                    for (WfCcTask ccTask : ccTasks) {
                        sendCcNotifyAsync(ccTask, instance);
                    }
                }
            }
        } catch (Exception e) {
            log.error("节点启动创建抄送任务失败, instanceId={}, error={}", instance.getId(), e.getMessage(), e);
        }
    }

    private void createCcTasksOnNodeComplete(WfProcessInstance instance, WfNodeConfig nodeConfig, Task task) {
        try {
            if (nodeConfig == null || nodeConfig.getNotifyConfig() == null) {
                return;
            }

            Map<String, Object> notifyConfig = nodeConfig.getNotifyConfig();
            Object nodeCompleteCcObj = notifyConfig.get("nodeCompleteCc");

            if (nodeCompleteCcObj != null) {
                List<Long> ccUserIds = ccUserResolver.resolveCcUsers(
                        nodeCompleteCcObj, instance.getStartUserId(), instance.getStartDeptId());

                if (ccUserIds != null && !ccUserIds.isEmpty()) {
                    List<WfCcTask> ccTasks = createAndSaveCcTasks(
                            instance, nodeConfig, ccUserIds, CcTypeEnum.AUTO_NODE_COMPLETE);

                    for (WfCcTask ccTask : ccTasks) {
                        sendCcNotifyAsync(ccTask, instance);
                    }
                }
            }
        } catch (Exception e) {
            log.error("节点完成创建抄送任务失败, instanceId={}, nodeId={}, error={}",
                    instance.getId(), nodeConfig.getNodeId(), e.getMessage(), e);
        }
    }

    private List<WfCcTask> createAndSaveCcTasks(WfProcessInstance instance, WfNodeConfig nodeConfig,
                                                List<Long> ccUserIds, CcTypeEnum ccType) {
        String detailUrl = frontendConfig.getApprovalDetailUrl(instance.getId());
        List<WfCcTask> ccTasks = new ArrayList<>();

        for (Long ccUserId : ccUserIds) {
            WfCcTask ccTask = new WfCcTask();
            ccTask.setInstanceId(instance.getId());
            ccTask.setProcessKey(instance.getProcessKey());
            ccTask.setCcUserId(ccUserId);
            ccTask.setNodeId(nodeConfig.getNodeId());
            ccTask.setNodeName(nodeConfig.getNodeName());
            ccTask.setCcType(ccType.getCode());
            ccTask.setIsRead(0);
            ccTask.setCcTime(LocalDateTime.now());
            ccTask.setRemindCount(0);
            ccTask.setDetailUrl(detailUrl);
            ccTasks.add(ccTask);
        }

        ccTaskService.saveBatch(ccTasks);
        log.info("创建抄送任务成功, instanceId={}, nodeId={}, ccType={}, ccUserCount={}",
                instance.getId(), nodeConfig.getNodeId(), ccType.getName(), ccUserIds.size());

        return ccTasks;
    }

    @Async
    protected void sendTaskCreateNotify(Task task, WfProcessInstance instance, WfNodeConfig nodeConfig) {
        try {
            Long assigneeId = parseAssigneeId(task.getAssignee());
            if (assigneeId == null) {
                log.warn("任务审批人为空，跳过通知, taskId={}", task.getId());
                return;
            }

            NotifySendDTO sendDTO = buildNotifySendDTO(instance, task.getId(), assigneeId,
                    EventTypeEnum.TASK_CREATE.getCode(), task, nodeConfig);
            notifyService.sendNotify(sendDTO);
            log.info("任务创建通知已发送, taskId={}, assigneeId={}", task.getId(), assigneeId);
        } catch (Exception e) {
            log.error("发送任务创建通知失败, taskId={}, error={}", task.getId(), e.getMessage(), e);
        }
    }

    private NotifySendDTO buildNotifySendDTO(WfProcessInstance instance, String taskId, Long receiverUserId,
                                             String eventType, Task task, WfNodeConfig nodeConfig) {
        NotifySendDTO sendDTO = new NotifySendDTO();
        sendDTO.setEventType(eventType);
        sendDTO.setBusinessType("WORKFLOW");
        sendDTO.setInstanceId(instance.getId());
        if (taskId != null) {
            WfApprovalTask approvalTask = approvalTaskService.getByFlowableTaskId(taskId);
            if (approvalTask != null) {
                sendDTO.setTaskId(approvalTask.getId());
            }
        }
        sendDTO.setReceiverUserId(receiverUserId);

        Map<String, Object> params = new HashMap<>();
        params.put("processTitle", instance.getTitle());
        params.put("instanceNo", instance.getInstanceNo());
        params.put("processKey", instance.getProcessKey());
        params.put("startUserId", instance.getStartUserId());
        params.put("nodeId", task != null ? task.getTaskDefinitionKey() : (nodeConfig != null ? nodeConfig.getNodeId() : null));
        params.put("nodeName", nodeConfig != null ? nodeConfig.getNodeName() : (task != null ? task.getName() : null));
        params.put("receiverUserId", receiverUserId);
        params.put("taskId", taskId);
        params.put("instanceId", instance.getId());
        sendDTO.setParams(params);

        return sendDTO;
    }

    private void handleTaskAssignment(Task task) {
        log.info("任务分配: taskId={}, assignee={}", task.getId(), task.getAssignee());

        try {
            WfApprovalTask approvalTask = approvalTaskService.getByFlowableTaskId(task.getId());
            if (approvalTask != null) {
                Long newAssigneeId = parseAssigneeId(task.getAssignee());
                if (newAssigneeId != null && !newAssigneeId.equals(approvalTask.getAssigneeId())) {
                    approvalTask.setAssigneeId(newAssigneeId);
                    approvalTask.setAssignTime(LocalDateTime.now());
                    approvalTaskService.updateById(approvalTask);
                    log.info("更新待办任务审批人, taskId={}, oldAssignee={}, newAssignee={}",
                            approvalTask.getId(), approvalTask.getAssigneeId(), newAssigneeId);
                }
            }
        } catch (Exception e) {
            log.error("任务分配处理失败, taskId={}, error={}", task.getId(), e.getMessage(), e);
        }
    }

    private void handleTaskComplete(Task task) {
        log.info("任务完成: taskId={}, taskName={}", task.getId(), task.getName());

        try {
            WfApprovalTask approvalTask = approvalTaskService.getByFlowableTaskId(task.getId());
            if (approvalTask != null && TaskStatusEnum.PENDING.getCode().equals(approvalTask.getTaskStatus())) {
                approvalTask.setTaskStatus(TaskStatusEnum.DONE.getCode());
                if (approvalTask.getAssignTime() != null) {
                    approvalTask.setActionDuration(ChronoUnit.MILLIS.between(approvalTask.getAssignTime(), LocalDateTime.now()));
                }
                approvalTask.setActionTime(LocalDateTime.now());
                approvalTaskService.updateById(approvalTask);
                log.info("更新待办任务状态为已完成, flowableTaskId={}", task.getId());
            }

            WfProcessInstance instance = processInstanceService.getByFlowableInstId(task.getProcessInstanceId());
            if (instance != null) {
                WfNodeConfig nodeConfig = null;
                if (instance.getProcessVersionId() != null) {
                    nodeConfig = nodeConfigService.getByNodeId(
                            instance.getProcessVersionId(), task.getTaskDefinitionKey());
                }
                if (nodeConfig != null) {
                    createCcTasksOnNodeComplete(instance, nodeConfig, task);
                }
            }

            sendTaskCompleteNotify(task);

        } catch (Exception e) {
            log.error("任务完成处理失败, taskId={}, error={}", task.getId(), e.getMessage(), e);
        }
    }

    @Async
    protected void sendTaskCompleteNotify(Task task) {
        try {
            WfProcessInstance instance = processInstanceService.getByFlowableInstId(task.getProcessInstanceId());
            if (instance == null) {
                return;
            }

            HistoricTaskInstance historicTask = historyService.createHistoricTaskInstanceQuery()
                    .taskId(task.getId())
                    .singleResult();
            String deleteReason = historicTask != null ? historicTask.getDeleteReason() : null;

            Long startUserId = instance.getStartUserId();
            if (startUserId == null) {
                return;
            }

            NotifySendDTO sendDTO = buildNotifySendDTO(instance, task.getId(), startUserId,
                    EventTypeEnum.TASK_COMPLETE.getCode(), task, null);
            if (sendDTO.getParams() == null) {
                sendDTO.setParams(new HashMap<>());
            }
            sendDTO.getParams().put("actionResult", StrUtil.isNotBlank(deleteReason) ? deleteReason : "已处理");
            notifyService.sendNotify(sendDTO);
            log.info("任务完成通知已发送, taskId={}, startUserId={}", task.getId(), startUserId);
        } catch (Exception e) {
            log.error("发送任务完成通知失败, taskId={}, error={}", task.getId(), e.getMessage(), e);
        }
    }

    private void handleTaskDelete(Task task) {
        log.info("任务删除: taskId={}, deleteReason={}", task.getId(), task.getDeleteReason());

        try {
            WfApprovalTask approvalTask = approvalTaskService.getByFlowableTaskId(task.getId());
            if (approvalTask != null && TaskStatusEnum.PENDING.getCode().equals(approvalTask.getTaskStatus())) {
                approvalTask.setTaskStatus(TaskStatusEnum.CANCELED.getCode());
                approvalTask.setActionTime(LocalDateTime.now());
                approvalTaskService.updateById(approvalTask);
                log.info("更新待办任务状态为已取消, flowableTaskId={}", task.getId());
            }
        } catch (Exception e) {
            log.error("任务删除处理失败, taskId={}, error={}", task.getId(), e.getMessage(), e);
        }
    }

    private String generateTaskNo() {
        return "TK" + System.currentTimeMillis() + IdUtil.randomUUID().substring(0, 4).toUpperCase();
    }

    @Async
    protected void sendCcNotifyAsync(WfCcTask ccTask, WfProcessInstance instance) {
        try {
            NotifySendDTO sendDTO = buildCcNotifySendDTO(ccTask, instance);
            notifyService.sendNotify(sendDTO);
            log.info("抄送通知已发送, ccTaskId={}, ccUserId={}", ccTask.getId(), ccTask.getCcUserId());
        } catch (Exception e) {
            log.error("发送抄送通知失败, ccTaskId={}, error={}", ccTask.getId(), e.getMessage(), e);
        }
    }

    private NotifySendDTO buildCcNotifySendDTO(WfCcTask ccTask, WfProcessInstance instance) {
        NotifySendDTO sendDTO = new NotifySendDTO();
        sendDTO.setEventType(EventTypeEnum.CC_NOTIFY.getCode());
        sendDTO.setBusinessType("WORKFLOW");
        sendDTO.setInstanceId(ccTask.getInstanceId());
        sendDTO.setReceiverUserId(ccTask.getCcUserId());

        Map<String, Object> params = new HashMap<>();
        params.put("processTitle", instance.getTitle());
        params.put("instanceNo", instance.getInstanceNo());
        params.put("processKey", instance.getProcessKey());
        params.put("startUserId", instance.getStartUserId());
        params.put("nodeId", ccTask.getNodeId());
        params.put("nodeName", ccTask.getNodeName());
        params.put("receiverUserId", ccTask.getCcUserId());
        params.put("instanceId", instance.getId());
        params.put("detailUrl", ccTask.getDetailUrl());

        sendDTO.setParams(params);
        return sendDTO;
    }

    @Override
    public boolean isFailOnException() {
        return false;
    }

    @Override
    public boolean isFireOnTransactionLifecycleEvent() {
        return false;
    }

    @Override
    public String getOnTransaction() {
        return null;
    }
}
