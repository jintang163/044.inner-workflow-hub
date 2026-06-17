package com.innerworkflow.api.config;

import com.innerworkflow.bpmn.listener.GlobalProcessListener;
import com.innerworkflow.bpmn.listener.GlobalTaskListener;
import lombok.RequiredArgsConstructor;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
@RequiredArgsConstructor
public class FlowableConfig {

    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final GlobalProcessListener globalProcessListener;
    private final GlobalTaskListener globalTaskListener;

    @PostConstruct
    public void registerListeners() {
        runtimeService.addEventListener(globalProcessListener,
                FlowableEngineEventType.PROCESS_STARTED,
                FlowableEngineEventType.PROCESS_COMPLETED,
                FlowableEngineEventType.PROCESS_CANCELLED,
                FlowableEngineEventType.ACTIVITY_STARTED,
                FlowableEngineEventType.ACTIVITY_COMPLETED);

        taskService.addEventListener(globalTaskListener,
                FlowableEngineEventType.TASK_CREATED,
                FlowableEngineEventType.TASK_ASSIGNED,
                FlowableEngineEventType.TASK_COMPLETED,
                FlowableEngineEventType.TASK_DELETED);
    }
}
