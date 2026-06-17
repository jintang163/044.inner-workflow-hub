package com.innerworkflow.bpmn.handler;

import com.innerworkflow.bpmn.entity.WfNodeConfig;
import com.innerworkflow.bpmn.service.WfNodeConfigService;
import com.innerworkflow.bpmn.service.WfProcessVersionService;
import com.innerworkflow.common.enums.ResultCode;
import com.innerworkflow.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.task.service.delegate.DelegateTask;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskAssigneeHandler implements TaskListener {

    private final Map<Integer, AssigneeResolver> assigneeResolverMap = new HashMap<>();
    private final WfNodeConfigService nodeConfigService;
    private final WfProcessVersionService processVersionService;

    public void registerResolver(AssigneeResolver resolver) {
        assigneeResolverMap.put(resolver.getAssigneeType(), resolver);
    }

    @Override
    public void notify(DelegateTask delegateTask) {
        String processDefinitionId = delegateTask.getProcessDefinitionId();
        String taskDefinitionKey = delegateTask.getTaskDefinitionKey();

        Long processVersionId = getProcessVersionIdByFlowableDefId(processDefinitionId);
        if (processVersionId == null) {
            log.warn("未找到流程版本, flowableProcessDefId={}", processDefinitionId);
            return;
        }

        WfNodeConfig nodeConfig = nodeConfigService.getByNodeId(processVersionId, taskDefinitionKey);
        if (nodeConfig == null) {
            log.warn("未找到节点配置, processVersionId={}, nodeId={}", processVersionId, taskDefinitionKey);
            return;
        }

        Long startUserId = getStartUserId(delegateTask);
        Long startDeptId = getStartDeptId(delegateTask);

        AssigneeResolver resolver = assigneeResolverMap.get(nodeConfig.getAssigneeType());
        if (resolver == null) {
            log.error("不支持的审批人类型: {}, nodeId={}", nodeConfig.getAssigneeType(), taskDefinitionKey);
            handleEmptyAssignee(nodeConfig, delegateTask);
            return;
        }

        List<Long> assignees = resolver.resolve(nodeConfig, startUserId, startDeptId);
        if (assignees == null || assignees.isEmpty()) {
            log.warn("审批人为空, nodeId={}", taskDefinitionKey);
            handleEmptyAssignee(nodeConfig, delegateTask);
            return;
        }

        if (assignees.size() == 1) {
            delegateTask.setAssignee(String.valueOf(assignees.get(0)));
        } else {
            for (Long assignee : assignees) {
                delegateTask.addCandidateUser(String.valueOf(assignee));
            }
        }

        delegateTask.setName(nodeConfig.getNodeName());
        delegateTask.setDescription(nodeConfig.getNodeName());
    }

    private Long getProcessVersionIdByFlowableDefId(String flowableProcessDefId) {
        return null;
    }

    private Long getStartUserId(DelegateTask delegateTask) {
        Object startUserId = delegateTask.getVariable("startUserId");
        if (startUserId instanceof Long) {
            return (Long) startUserId;
        } else if (startUserId instanceof String) {
            try {
                return Long.parseLong((String) startUserId);
            } catch (NumberFormatException e) {
                return null;
            }
        } else if (startUserId instanceof Number) {
            return ((Number) startUserId).longValue();
        }
        return null;
    }

    private Long getStartDeptId(DelegateTask delegateTask) {
        Object startDeptId = delegateTask.getVariable("startDeptId");
        if (startDeptId instanceof Long) {
            return (Long) startDeptId;
        } else if (startDeptId instanceof String) {
            try {
                return Long.parseLong((String) startDeptId);
            } catch (NumberFormatException e) {
                return null;
            }
        } else if (startDeptId instanceof Number) {
            return ((Number) startDeptId).longValue();
        }
        return null;
    }

    private void handleEmptyAssignee(WfNodeConfig nodeConfig, DelegateTask delegateTask) {
        Integer strategy = nodeConfig.getEmptyAssigneeStrategy();
        if (strategy == null) {
            strategy = 1;
        }

        switch (strategy) {
            case 1:
                log.info("审批人为空，自动通过, nodeId={}", nodeConfig.getNodeId());
                break;
            case 2:
                log.info("审批人为空，转管理员, nodeId={}", nodeConfig.getNodeId());
                break;
            case 3:
                throw new BusinessException(ResultCode.PARAM_ERROR,
                        "节点\"" + nodeConfig.getNodeName() + "\"审批人为空");
            default:
                break;
        }
    }
}
