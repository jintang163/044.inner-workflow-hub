package com.innerworkflow.bpmn.listener;

import cn.hutool.core.util.StrUtil;
import com.innerworkflow.approval.dto.WfCcAddDTO;
import com.innerworkflow.approval.entity.WfProcessInstance;
import com.innerworkflow.approval.entity.WfProcessInstanceRelation;
import com.innerworkflow.approval.enums.CcTypeEnum;
import com.innerworkflow.approval.handler.CcUserResolver;
import com.innerworkflow.approval.service.WfCcTaskService;
import com.innerworkflow.approval.service.WfProcessInstanceRelationService;
import com.innerworkflow.approval.service.WfProcessInstanceService;
import com.innerworkflow.bpmn.entity.WfNodeConfig;
import com.innerworkflow.bpmn.entity.WfProcessVersion;
import com.innerworkflow.bpmn.service.WfNodeConfigService;
import com.innerworkflow.bpmn.service.WfProcessVersionService;
import com.innerworkflow.common.enums.InstanceStatusEnum;
import com.innerworkflow.notify.dto.NotifySendDTO;
import com.innerworkflow.notify.enums.EventTypeEnum;
import com.innerworkflow.notify.service.WfNotifyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.CallActivity;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.HistoryService;
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
public class GlobalProcessListener implements FlowableEventListener, ExecutionListener {

    private final WfProcessInstanceService processInstanceService;
    private final WfNotifyService notifyService;
    private final WfCcTaskService ccTaskService;
    private final WfProcessVersionService processVersionService;
    private final CcUserResolver ccUserResolver;
    private final HistoryService historyService;
    private final RepositoryService repositoryService;
    private final WfNodeConfigService nodeConfigService;
    private final WfProcessInstanceRelationService processInstanceRelationService;
    private final org.flowable.engine.RuntimeService runtimeService;

    @Override
    public void onEvent(FlowableEvent event) {
        String eventType = event.getType().name();

        if (event instanceof FlowableEngineEntityEvent) {
            FlowableEngineEntityEvent entityEvent = (FlowableEngineEntityEvent) event;
            Object entity = entityEvent.getEntity();

            try {
                if (entity instanceof ProcessInstance) {
                    ProcessInstance processInstance = (ProcessInstance) entity;
                    handleProcessInstanceEvent(eventType, processInstance);
                } else if (entity instanceof ProcessDefinition) {
                    ProcessDefinition processDefinition = (ProcessDefinition) entity;
                    handleProcessDefinitionEvent(eventType, processDefinition);
                }
            } catch (Exception e) {
                log.error("全局流程监听器处理异常, eventType={}, entity={}, error={}",
                        eventType, entity != null ? entity.getClass().getSimpleName() : "null", e.getMessage(), e);
            }
        }
    }

    private void handleProcessInstanceEvent(String eventType, ProcessInstance processInstance) {
        log.info("流程实例事件 - 事件: {}, 流程实例ID: {}, 流程定义ID: {}, 业务Key: {}",
                eventType, processInstance.getId(), processInstance.getProcessDefinitionId(),
                processInstance.getBusinessKey());

        if (FlowableEngineEventType.PROCESS_STARTED.name().equals(eventType)) {
            handleProcessStart(processInstance);
        } else if (FlowableEngineEventType.PROCESS_COMPLETED.name().equals(eventType)) {
            handleProcessEnd(processInstance);
        } else if (FlowableEngineEventType.PROCESS_CANCELLED.name().equals(eventType)) {
            handleProcessCancelled(processInstance);
        }
    }

    private void handleProcessDefinitionEvent(String eventType, ProcessDefinition processDefinition) {
        log.debug("流程定义事件 - 事件: {}, 流程定义ID: {}, Key: {}, 版本: {}",
                eventType, processDefinition.getId(), processDefinition.getKey(),
                processDefinition.getVersion());
    }

    private void handleProcessStart(ProcessInstance processInstance) {
        log.info("流程开始: processInstanceId={}, processDefinitionKey={}",
                processInstance.getId(), processInstance.getProcessDefinitionKey());

        try {
            sendProcessStartNotify(processInstance);

            handleSubProcessStart(processInstance);
        } catch (Exception e) {
            log.error("流程开始处理失败, processInstanceId={}, error={}",
                    processInstance.getId(), e.getMessage(), e);
        }
    }

    private void handleSubProcessStart(ProcessInstance processInstance) {
        String superExecutionId = processInstance.getSuperExecutionId();
        if (superExecutionId == null) {
            return;
        }

        log.info("检测到子流程启动, childProcessInstId={}, superExecutionId={}",
                processInstance.getId(), superExecutionId);

        Execution superExecution = runtimeService.createExecutionQuery()
                .executionId(superExecutionId)
                .singleResult();
        if (superExecution == null) {
            log.warn("未找到父执行流, superExecutionId={}", superExecutionId);
            return;
        }

        String parentFlowableInstId = superExecution.getProcessInstanceId();
        WfProcessInstance parentInstance = processInstanceService.getByFlowableInstId(parentFlowableInstId);
        if (parentInstance == null) {
            log.warn("未找到父流程实例, parentFlowableInstId={}", parentFlowableInstId);
            return;
        }

        ProcessDefinition processDef = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(processInstance.getProcessDefinitionId())
                .singleResult();

        String callActivityNodeId = superExecution.getActivityId();
        String callActivityNodeName = callActivityNodeId;

        try {
            org.flowable.bpmn.model.BpmnModel bpmnModel = repositoryService.getBpmnModel(
                    parentInstance.getFlowableProcessDefId());
            FlowElement flowElement = bpmnModel.getProcesses().get(0)
                    .getFlowElement(callActivityNodeId);
            if (flowElement instanceof CallActivity callActivity) {
                callActivityNodeName = callActivity.getName() != null ?
                        callActivity.getName() : callActivityNodeId;
            }
        } catch (Exception e) {
            log.warn("获取CallActivity名称失败, nodeId={}, error={}", callActivityNodeId, e.getMessage());
        }

        WfProcessInstanceRelation relation = new WfProcessInstanceRelation();
        relation.setParentInstanceId(parentInstance.getId());
        relation.setParentFlowableInstId(parentFlowableInstId);
        relation.setParentNodeId(callActivityNodeId);
        relation.setChildFlowableInstId(processInstance.getId());
        relation.setChildProcessKey(processInstance.getProcessDefinitionKey());
        relation.setChildProcessName(processDef != null ? processDef.getName() : processInstance.getProcessDefinitionKey());
        relation.setCallActivityNodeId(callActivityNodeId);
        relation.setCallActivityNodeName(callActivityNodeName);
        relation.setRelationType(1);
        relation.setCreateTime(LocalDateTime.now());
        processInstanceRelationService.save(relation);

        log.info("已建立主子流程关联, parentInstanceId={}, childFlowableInstId={}",
                parentInstance.getId(), processInstance.getId());
    }

    @Async
    protected void sendProcessStartNotify(ProcessInstance processInstance) {
        try {
            WfProcessInstance instance = processInstanceService.getByFlowableInstId(processInstance.getId());
            if (instance == null) {
                log.warn("流程开始通知-未找到本地流程实例, flowableInstId={}", processInstance.getId());
                return;
            }

            Long startUserId = instance.getStartUserId();
            if (startUserId == null) {
                return;
            }

            NotifySendDTO sendDTO = buildProcessNotifySendDTO(instance, EventTypeEnum.PROCESS_START.getCode());
            notifyService.sendNotify(sendDTO);
            log.info("流程开始通知已发送, instanceId={}, startUserId={}", instance.getId(), startUserId);
        } catch (Exception e) {
            log.error("发送流程开始通知失败, processInstanceId={}, error={}",
                    processInstance.getId(), e.getMessage(), e);
        }
    }

    private void handleProcessEnd(ProcessInstance processInstance) {
        log.info("流程结束: processInstanceId={}", processInstance.getId());

        try {
            WfProcessInstance instance = processInstanceService.getByFlowableInstId(processInstance.getId());
            if (instance == null) {
                log.warn("流程结束-未找到本地流程实例, flowableInstId={}", processInstance.getId());
                return;
            }

            HistoricProcessInstance historicInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .singleResult();

            String deleteReason = historicInstance != null ? historicInstance.getDeleteReason() : null;
            if (StrUtil.isNotBlank(deleteReason)) {
                if (deleteReason.contains("驳回")) {
                    instance.setInstanceStatus(InstanceStatusEnum.REJECTED.getCode());
                } else if (deleteReason.contains("撤回") || deleteReason.contains("取消")) {
                    instance.setInstanceStatus(InstanceStatusEnum.CANCELED.getCode());
                } else {
                    instance.setInstanceStatus(InstanceStatusEnum.APPROVED.getCode());
                }
            } else {
                instance.setInstanceStatus(InstanceStatusEnum.APPROVED.getCode());
            }

            LocalDateTime endTime = LocalDateTime.now();
            instance.setEndTime(endTime);
            if (instance.getStartTime() != null) {
                instance.setDuration(ChronoUnit.MILLIS.between(instance.getStartTime(), endTime));
            }
            instance.setCurrentNodeIds(null);
            instance.setCurrentApproverIds(null);
            processInstanceService.updateById(instance);
            log.info("流程状态已更新为结束, instanceId={}, status={}", instance.getId(), instance.getInstanceStatus());

            createProcessEndCcTasks(instance);

            sendProcessEndNotify(instance);
        } catch (Exception e) {
            log.error("流程结束处理失败, processInstanceId={}, error={}",
                    processInstance.getId(), e.getMessage(), e);
        }
    }

    @Async
    protected void sendProcessEndNotify(WfProcessInstance instance) {
        try {
            Long startUserId = instance.getStartUserId();
            if (startUserId == null) {
                return;
            }

            NotifySendDTO sendDTO = buildProcessNotifySendDTO(instance, EventTypeEnum.PROCESS_END.getCode());
            if (sendDTO.getParams() == null) {
                sendDTO.setParams(new HashMap<>());
            }
            InstanceStatusEnum statusEnum = InstanceStatusEnum.getByCode(instance.getInstanceStatus());
            sendDTO.getParams().put("processResult", statusEnum != null ? statusEnum.getDesc() : "已结束");
            notifyService.sendNotify(sendDTO);
            log.info("流程结束通知已发送, instanceId={}, startUserId={}", instance.getId(), startUserId);
        } catch (Exception e) {
            log.error("发送流程结束通知失败, instanceId={}, error={}",
                    instance.getId(), e.getMessage(), e);
        }
    }

    private void handleProcessCancelled(ProcessInstance processInstance) {
        log.info("流程取消: processInstanceId={}", processInstance.getId());

        try {
            WfProcessInstance instance = processInstanceService.getByFlowableInstId(processInstance.getId());
            if (instance == null) {
                log.warn("流程取消-未找到本地流程实例, flowableInstId={}", processInstance.getId());
                return;
            }

            if (!InstanceStatusEnum.CANCELED.getCode().equals(instance.getInstanceStatus())) {
                instance.setInstanceStatus(InstanceStatusEnum.CANCELED.getCode());
                LocalDateTime endTime = LocalDateTime.now();
                instance.setEndTime(endTime);
                if (instance.getStartTime() != null) {
                    instance.setDuration(ChronoUnit.MILLIS.between(instance.getStartTime(), endTime));
                }
                instance.setCurrentNodeIds(null);
                instance.setCurrentApproverIds(null);
                processInstanceService.updateById(instance);
                log.info("流程状态已更新为已取消, instanceId={}", instance.getId());
            }
        } catch (Exception e) {
            log.error("流程取消处理失败, processInstanceId={}, error={}",
                    processInstance.getId(), e.getMessage(), e);
        }
    }

    private NotifySendDTO buildProcessNotifySendDTO(WfProcessInstance instance, String eventType) {
        NotifySendDTO sendDTO = new NotifySendDTO();
        sendDTO.setEventType(eventType);
        sendDTO.setBusinessType("WORKFLOW");
        sendDTO.setInstanceId(instance.getId());
        sendDTO.setReceiverUserId(instance.getStartUserId());

        Map<String, Object> params = new HashMap<>();
        params.put("processTitle", instance.getTitle());
        params.put("instanceNo", instance.getInstanceNo());
        params.put("processKey", instance.getProcessKey());
        params.put("startUserId", instance.getStartUserId());
        params.put("instanceId", instance.getId());
        params.put("startTime", instance.getStartTime());
        params.put("endTime", instance.getEndTime());
        params.put("duration", instance.getDuration());
        sendDTO.setParams(params);

        return sendDTO;
    }

    private void createProcessEndCcTasks(WfProcessInstance instance) {
        try {
            WfProcessVersion processVersion = processVersionService.getById(instance.getProcessVersionId());
            if (processVersion == null || processVersion.getGlobalNotifyConfig() == null) {
                return;
            }

            Map<String, Object> globalNotifyConfig = processVersion.getGlobalNotifyConfig();
            Object processEndCcObj = globalNotifyConfig.get("processEndCc");

            if (processEndCcObj == null) {
                return;
            }

            List<Long> ccUserIds = ccUserResolver.resolveCcUsers(
                    processEndCcObj, instance.getStartUserId(), instance.getStartDeptId());

            if (ccUserIds == null || ccUserIds.isEmpty()) {
                return;
            }

            WfCcAddDTO addDTO = new WfCcAddDTO();
            addDTO.setInstanceId(instance.getId());
            addDTO.setCcUserIds(ccUserIds);
            addDTO.setNodeId("PROCESS_END");
            addDTO.setNodeName("流程结束");
            addDTO.setCcType(CcTypeEnum.AUTO_PROCESS_END.getCode());
            ccTaskService.addCcInternal(addDTO);
        } catch (Exception e) {
            log.error("流程结束创建抄送任务失败, instanceId={}, error={}", instance.getId(), e.getMessage(), e);
        }
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

    @Override
    public void notify(DelegateExecution execution) {
        String eventName = execution.getEventName();
        String activityId = execution.getCurrentActivityId();
        String processInstId = execution.getProcessInstanceId();

        log.info("CallActivity执行监听器触发, event={}, activityId={}, processInstId={}",
                eventName, activityId, processInstId);

        try {
            if ("start".equals(eventName)) {
                handleCallActivityStart(execution);
            } else if ("end".equals(eventName)) {
                handleCallActivityEnd(execution);
            }
        } catch (Exception e) {
            log.error("CallActivity监听器处理失败, event={}, activityId={}, error={}",
                    eventName, activityId, e.getMessage(), e);
        }
    }

    private void handleCallActivityStart(DelegateExecution execution) {
        String activityId = execution.getCurrentActivityId();
        String processDefId = execution.getProcessDefinitionId();
        String processInstId = execution.getProcessInstanceId();

        log.info("CallActivity开始, activityId={}, processInstId={}", activityId, processInstId);

        WfProcessInstance instance = processInstanceService.getByFlowableInstId(processInstId);
        if (instance == null) {
            return;
        }

        WfNodeConfig nodeConfig = nodeConfigService.getByNodeId(
                instance.getProcessVersionId(), activityId);
        if (nodeConfig != null && nodeConfig.getNotifyConfig() != null) {
            log.info("CallActivity节点通知配置, nodeId={}, notifyConfig={}",
                    activityId, nodeConfig.getNotifyConfig());
        }
    }

    private void handleCallActivityEnd(DelegateExecution execution) {
        String activityId = execution.getCurrentActivityId();
        String processInstId = execution.getProcessInstanceId();

        log.info("CallActivity结束, activityId={}, processInstId={}", activityId, processInstId);

        WfProcessInstance instance = processInstanceService.getByFlowableInstId(processInstId);
        if (instance == null) {
            return;
        }

        Map<String, Object> variables = runtimeService.getVariables(execution.getId());
        log.debug("CallActivity结束变量, variables={}", variables);

        Object parallelGatewayRejected = variables.get("parallelGatewayRejected");
        if (parallelGatewayRejected != null && Boolean.TRUE.equals(parallelGatewayRejected)) {
            log.info("检测到并行网关驳回标记, processInstId={}", processInstId);
        }
    }
}
