package com.innerworkflow.approval.cmd;

import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.flowable.task.service.impl.persistence.entity.TaskEntityManager;

import java.util.List;

public class AddSignBeforeTaskCmd implements Command<Void> {

    private final String taskId;
    private final List<String> assigneeList;
    private final String signType;

    public AddSignBeforeTaskCmd(String taskId, List<String> assigneeList, String signType) {
        this.taskId = taskId;
        this.assigneeList = assigneeList;
        this.signType = signType;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        TaskEntityManager taskEntityManager = CommandContextUtil.getTaskEntityManager(commandContext);
        TaskEntity currentTask = taskEntityManager.findById(taskId);

        if (currentTask == null) {
            throw new RuntimeException("任务不存在: " + taskId);
        }

        for (int i = 0; i < assigneeList.size(); i++) {
            String assignee = assigneeList.get(i);
            TaskEntity addSignTask = taskEntityManager.createTask();
            addSignTask.setName(currentTask.getName() + "(加签)");
            addSignTask.setProcessInstanceId(currentTask.getProcessInstanceId());
            addSignTask.setExecutionId(currentTask.getExecutionId());
            addSignTask.setTaskDefinitionKey(currentTask.getTaskDefinitionKey() + "_addSign_" + i);
            addSignTask.setAssignee(assignee);
            addSignTask.setPriority(currentTask.getPriority());
            addSignTask.setScopeId(currentTask.getScopeId());
            addSignTask.setScopeType(currentTask.getScopeType());
            taskEntityManager.insert(addSignTask);
        }

        return null;
    }
}
