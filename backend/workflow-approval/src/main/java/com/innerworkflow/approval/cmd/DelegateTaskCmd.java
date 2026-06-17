package com.innerworkflow.approval.cmd;

import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.flowable.task.service.impl.persistence.entity.TaskEntityManager;

public class DelegateTaskCmd implements Command<Void> {

    private final String taskId;
    private final String delegateAssignee;
    private final String remark;

    public DelegateTaskCmd(String taskId, String delegateAssignee, String remark) {
        this.taskId = taskId;
        this.delegateAssignee = delegateAssignee;
        this.remark = remark;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        TaskEntityManager taskEntityManager = CommandContextUtil.getTaskEntityManager(commandContext);
        TaskEntity task = taskEntityManager.findById(taskId);

        if (task == null) {
            throw new RuntimeException("任务不存在: " + taskId);
        }

        task.setDelegationState(org.flowable.task.api.DelegationState.PENDING);
        task.setOwner(task.getAssignee());
        task.setAssignee(delegateAssignee);

        return null;
    }
}
