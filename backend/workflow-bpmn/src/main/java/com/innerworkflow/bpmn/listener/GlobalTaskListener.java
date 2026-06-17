package com.innerworkflow.bpmn.listener;

import lombok.extern.slf4j.Slf4j;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GlobalTaskListener implements FlowableEventListener {

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

        if (FlowableEngineEventType.TASK_CREATED.name().equals(eventType)) {
            handleTaskCreate(task);
        } else if (FlowableEngineEventType.TASK_ASSIGNED.name().equals(eventType)) {
            handleTaskAssignment(task);
        } else if (FlowableEngineEventType.TASK_COMPLETED.name().equals(eventType)) {
            handleTaskComplete(task);
        } else if (FlowableEngineEventType.TASK_DELETED.name().equals(eventType)) {
            handleTaskDelete(task);
        }
    }

    private void handleTaskCreate(Task task) {
        log.info("任务创建: taskId={}, taskName={}, processInstanceId={}",
                task.getId(), task.getName(), task.getProcessInstanceId());
    }

    private void handleTaskAssignment(Task task) {
        log.info("任务分配: taskId={}, assignee={}", task.getId(), task.getAssignee());
    }

    private void handleTaskComplete(Task task) {
        log.info("任务完成: taskId={}, taskName={}", task.getId(), task.getName());
    }

    private void handleTaskDelete(Task task) {
        log.info("任务删除: taskId={}, deleteReason={}",
                task.getId(), task.getDeleteReason());
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
