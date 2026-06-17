package com.innerworkflow.bpmn.listener;

import lombok.extern.slf4j.Slf4j;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GlobalProcessListener implements FlowableEventListener {

    @Override
    public void onEvent(FlowableEvent event) {
        String eventType = event.getType().name();

        if (event instanceof FlowableEngineEntityEvent) {
            FlowableEngineEntityEvent entityEvent = (FlowableEngineEntityEvent) event;
            Object entity = entityEvent.getEntity();

            if (entity instanceof ProcessInstance) {
                ProcessInstance processInstance = (ProcessInstance) entity;
                handleProcessInstanceEvent(eventType, processInstance);
            } else if (entity instanceof ProcessDefinition) {
                ProcessDefinition processDefinition = (ProcessDefinition) entity;
                handleProcessDefinitionEvent(eventType, processDefinition);
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
    }

    private void handleProcessEnd(ProcessInstance processInstance) {
        log.info("流程结束: processInstanceId={}", processInstance.getId());
    }

    private void handleProcessCancelled(ProcessInstance processInstance) {
        log.info("流程取消: processInstanceId={}", processInstance.getId());
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
