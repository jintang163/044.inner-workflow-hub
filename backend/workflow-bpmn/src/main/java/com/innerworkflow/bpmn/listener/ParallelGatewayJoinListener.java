package com.innerworkflow.bpmn.listener;

import com.innerworkflow.common.enums.ParallelGatewayRejectStrategyEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.ParallelGateway;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Slf4j
@Component("parallelGatewayJoinListener")
@RequiredArgsConstructor
public class ParallelGatewayJoinListener implements ExecutionListener {

    private final RuntimeService runtimeService;
    private final RepositoryService repositoryService;

    @Override
    public void notify(DelegateExecution execution) {
        try {
            String currentActivityId = execution.getCurrentActivityId();
            String processDefId = execution.getProcessDefinitionId();
            String processInstId = execution.getProcessInstanceId();

            log.info("并行网关汇聚执行监听器触发, gatewayId={}, processInstId={}", currentActivityId, processInstId);

            BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefId);
            if (bpmnModel == null) {
                return;
            }

            Collection<FlowElement> flowElements = bpmnModel.getProcesses().get(0).getFlowElements();
            ParallelGateway joinGateway = null;
            for (FlowElement element : flowElements) {
                if (element instanceof ParallelGateway && element.getId().equals(currentActivityId)) {
                    joinGateway = (ParallelGateway) element;
                    break;
                }
            }

            if (joinGateway == null) {
                return;
            }

            ParallelGateway forkGateway = findForkGatewayByJoinGateway(bpmnModel, joinGateway);
            if (forkGateway == null) {
                return;
            }

            String rejectVarName = "parallelReject_" + forkGateway.getId();
            Object rejectFlag = runtimeService.getVariable(processInstId, rejectVarName);
            if (rejectFlag != null && Boolean.TRUE.equals(rejectFlag)) {
                log.info("检测到并行网关驳回标记, forkGatewayId={}, processInstId={}", forkGateway.getId(), processInstId);

                Object rejectUserId = runtimeService.getVariable(processInstId, rejectVarName + "_userId");
                Object rejectRemark = runtimeService.getVariable(processInstId, rejectVarName + "_remark");

                runtimeService.setVariable(processInstId, "approved", false);
                runtimeService.setVariable(processInstId, "parallelGatewayRejected", true);
                if (rejectUserId != null) {
                    runtimeService.setVariable(processInstId, "rejectUserId", rejectUserId);
                }
                if (rejectRemark != null) {
                    runtimeService.setVariable(processInstId, "comment", rejectRemark);
                }

                log.info("已设置并行网关统一驳回变量, processInstId={}", processInstId);
            }

        } catch (Exception e) {
            log.error("并行网关汇聚监听器执行失败, executionId={}, error={}",
                    execution.getId(), e.getMessage(), e);
        }
    }

    private ParallelGateway findForkGatewayByJoinGateway(BpmnModel bpmnModel, ParallelGateway joinGateway) {
        Collection<FlowElement> flowElements = bpmnModel.getProcesses().get(0).getFlowElements();

        for (FlowElement element : flowElements) {
            if (element instanceof ParallelGateway) {
                ParallelGateway potentialFork = (ParallelGateway) element;
                if (potentialFork.getOutgoingFlows().size() >= 2) {
                    for (SequenceFlow outgoingFlow : potentialFork.getOutgoingFlows()) {
                        if (canReachGateway(outgoingFlow.getTargetRef(), joinGateway, new java.util.HashSet<>())) {
                            return potentialFork;
                        }
                    }
                }
            }
        }
        return null;
    }

    private boolean canReachGateway(FlowElement startElement, ParallelGateway targetGateway,
                                    java.util.Set<String> visited) {
        if (startElement == null || visited.contains(startElement.getId())) {
            return false;
        }
        visited.add(startElement.getId());

        if (startElement.getId().equals(targetGateway.getId())) {
            return true;
        }

        if (!(startElement instanceof org.flowable.bpmn.model.FlowNode)) {
            return false;
        }

        org.flowable.bpmn.model.FlowNode flowNode = (org.flowable.bpmn.model.FlowNode) startElement;
        for (SequenceFlow outgoing : flowNode.getOutgoingFlows()) {
            if (canReachGateway(outgoing.getTargetRef(), targetGateway, visited)) {
                return true;
            }
        }
        return false;
    }
}
