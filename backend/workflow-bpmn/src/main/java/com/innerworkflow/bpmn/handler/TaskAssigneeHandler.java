package com.innerworkflow.bpmn.handler;

import com.innerworkflow.bpmn.entity.WfNodeConfig;
import com.innerworkflow.bpmn.entity.WfProcessDefinition;
import com.innerworkflow.bpmn.entity.WfProcessVersion;
import com.innerworkflow.bpmn.service.WfNodeConfigService;
import com.innerworkflow.bpmn.service.WfProcessDefinitionService;
import com.innerworkflow.bpmn.service.WfProcessVersionService;
import com.innerworkflow.common.enums.ResultCode;
import com.innerworkflow.common.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.task.service.delegate.DelegateTask;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskAssigneeHandler implements TaskListener {

    private final Map<Integer, AssigneeResolver> assigneeResolverMap = new HashMap<>();
    private final List<AssigneeResolver> assigneeResolvers;
    private final WfNodeConfigService nodeConfigService;
    private final WfProcessVersionService processVersionService;
    private final WfProcessDefinitionService processDefinitionService;

    @PostConstruct
    public void init() {
        for (AssigneeResolver resolver : assigneeResolvers) {
            assigneeResolverMap.put(resolver.getAssigneeType(), resolver);
            log.info("注册审批人解析器: type={}, resolver={}", resolver.getAssigneeType(), resolver.getClass().getSimpleName());
        }
    }

    @Override
    public void notify(DelegateTask delegateTask) {
        String eventName = delegateTask.getEventName();
        if (!EVENTNAME_CREATE.equals(eventName)) {
            return;
        }

        String processDefinitionId = delegateTask.getProcessDefinitionId();
        String taskDefinitionKey = delegateTask.getTaskDefinitionKey();

        log.info("任务创建事件触发, processDefinitionId={}, taskDefinitionKey={}", processDefinitionId, taskDefinitionKey);

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

        if (nodeConfig.getNodeName() != null) {
            delegateTask.setName(nodeConfig.getNodeName());
            delegateTask.setDescription(nodeConfig.getNodeName());
        }

        if (nodeConfig.getTimeoutHours() != null && nodeConfig.getTimeoutHours() > 0) {
            LocalDateTime dueDate = LocalDateTime.now().plusHours(nodeConfig.getTimeoutHours());
            delegateTask.setDueDate(Date.from(dueDate.atZone(ZoneId.systemDefault()).toInstant()));
            log.info("设置任务超时时间, taskId={}, hours={}, dueDate={}", delegateTask.getId(), nodeConfig.getTimeoutHours(), dueDate);
        }

        Long startUserId = getStartUserId(delegateTask);
        Long startDeptId = getStartDeptId(delegateTask);

        AssigneeResolver resolver = assigneeResolverMap.get(nodeConfig.getAssigneeType());
        if (resolver == null) {
            log.error("不支持的审批人类型: {}, nodeId={}", nodeConfig.getAssigneeType(), taskDefinitionKey);
            handleEmptyAssignee(nodeConfig, delegateTask, processVersionId);
            return;
        }

        List<Long> assignees = resolver.resolve(nodeConfig, startUserId, startDeptId);
        if (assignees == null || assignees.isEmpty()) {
            log.warn("审批人为空, nodeId={}, assigneeType={}", taskDefinitionKey, nodeConfig.getAssigneeType());
            handleEmptyAssignee(nodeConfig, delegateTask, processVersionId);
            return;
        }

        setTaskAssignees(delegateTask, assignees, nodeConfig);
    }

    private void setTaskAssignees(DelegateTask delegateTask, List<Long> assignees, WfNodeConfig nodeConfig) {
        Integer multiInstance = nodeConfig.getMultiInstance();
        Integer approveType = nodeConfig.getApproveType();

        if (multiInstance != null && multiInstance == 1) {
            for (Long assignee : assignees) {
                delegateTask.addCandidateUser(String.valueOf(assignee));
            }
            log.info("多实例任务设置候选人, taskId={}, assignees={}", delegateTask.getId(), assignees);
        } else if (approveType != null && approveType == 1 && assignees.size() > 1) {
            for (Long assignee : assignees) {
                delegateTask.addCandidateUser(String.valueOf(assignee));
            }
            log.info("或签任务设置候选人, taskId={}, assignees={}", delegateTask.getId(), assignees);
        } else if (assignees.size() == 1) {
            delegateTask.setAssignee(String.valueOf(assignees.get(0)));
            log.info("设置任务处理人, taskId={}, assignee={}", delegateTask.getId(), assignees.get(0));
        } else {
            for (Long assignee : assignees) {
                delegateTask.addCandidateUser(String.valueOf(assignee));
            }
            log.info("任务设置多个候选人, taskId={}, assignees={}", delegateTask.getId(), assignees);
        }
    }

    private Long getProcessVersionIdByFlowableDefId(String flowableProcessDefId) {
        WfProcessVersion processVersion = processVersionService.getByFlowableProcessDefId(flowableProcessDefId);
        if (processVersion != null) {
            return processVersion.getId();
        }
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

    private void handleEmptyAssignee(WfNodeConfig nodeConfig, DelegateTask delegateTask, Long processVersionId) {
        Integer strategy = nodeConfig.getEmptyAssigneeStrategy();
        if (strategy == null) {
            strategy = 1;
        }

        switch (strategy) {
            case 1:
                log.info("审批人为空，自动跳过节点, nodeId={}, nodeName={}", nodeConfig.getNodeId(), nodeConfig.getNodeName());
                break;
            case 2:
                log.info("审批人为空，转管理员处理, nodeId={}, nodeName={}", nodeConfig.getNodeId(), nodeConfig.getNodeName());
                assignToAdmin(delegateTask, processVersionId);
                break;
            case 3:
                throw new BusinessException(ResultCode.PARAM_ERROR,
                        "节点\"" + nodeConfig.getNodeName() + "\"审批人为空");
            default:
                log.warn("未知的空审批人策略: {}, nodeId={}", strategy, nodeConfig.getNodeId());
                break;
        }
    }

    private void assignToAdmin(DelegateTask delegateTask, Long processVersionId) {
        try {
            WfProcessVersion processVersion = processVersionService.getById(processVersionId);
            if (processVersion != null) {
                WfProcessDefinition processDefinition = processDefinitionService.getById(processVersion.getProcessDefinitionId());
                if (processDefinition != null && processDefinition.getAdminUserIds() != null && !processDefinition.getAdminUserIds().isEmpty()) {
                    List<Long> adminIds = processDefinition.getAdminUserIds();
                    if (adminIds.size() == 1) {
                        delegateTask.setAssignee(String.valueOf(adminIds.get(0)));
                        log.info("转管理员审批, taskId={}, adminId={}", delegateTask.getId(), adminIds.get(0));
                    } else {
                        for (Long adminId : adminIds) {
                            delegateTask.addCandidateUser(String.valueOf(adminId));
                        }
                        log.info("转多个管理员审批, taskId={}, adminIds={}", delegateTask.getId(), adminIds);
                    }
                    return;
                }
            }
            log.warn("未配置流程管理员，无法转管理员处理, taskId={}", delegateTask.getId());
        } catch (Exception e) {
            log.error("转管理员处理失败, taskId={}", delegateTask.getId(), e);
        }
    }
}
