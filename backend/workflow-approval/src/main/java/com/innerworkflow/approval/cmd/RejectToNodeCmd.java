package com.innerworkflow.approval.cmd;

import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;

import java.util.List;

public class RejectToNodeCmd implements Command<Void> {

    private final String processInstanceId;
    private final String currentTaskId;
    private final String targetNodeId;
    private final String rejectReason;

    public RejectToNodeCmd(String processInstanceId, String currentTaskId, String targetNodeId, String rejectReason) {
        this.processInstanceId = processInstanceId;
        this.currentTaskId = currentTaskId;
        this.targetNodeId = targetNodeId;
        this.rejectReason = rejectReason;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        org.flowable.engine.impl.RuntimeServiceImpl runtimeService =
                (org.flowable.engine.impl.RuntimeServiceImpl) CommandContextUtil.getProcessEngineConfiguration(commandContext).getRuntimeService();

        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstanceId)
                .moveActivityIdTo(currentTaskId, targetNodeId)
                .changeState();

        return null;
    }
}
