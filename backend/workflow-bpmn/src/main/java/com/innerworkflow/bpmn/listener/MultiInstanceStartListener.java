package com.innerworkflow.bpmn.listener;

import com.innerworkflow.bpmn.entity.WfNodeConfig;
import com.innerworkflow.bpmn.entity.WfProcessVersion;
import com.innerworkflow.bpmn.handler.AssigneeResolver;
import com.innerworkflow.bpmn.service.WfNodeConfigService;
import com.innerworkflow.bpmn.service.WfProcessVersionService;
import com.innerworkflow.common.util.SpringContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.UserTask;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class MultiInstanceStartListener implements ExecutionListener {

    private static final Map<Integer, AssigneeResolver> resolverCache = new HashMap<>();

    @Override
    public void notify(DelegateExecution execution) {
        String eventName = execution.getEventName();
        String activityId = execution.getCurrentActivityId();
        String processDefinitionId = execution.getProcessDefinitionId();
        String processInstanceId = execution.getProcessInstanceId();

        if (!EVENTNAME_START.equals(eventName)) {
            return;
        }

        log.info("多实例启动监听器触发, event={}, activityId={}, processDefId={}, processInstId={}",
                eventName, activityId, processDefinitionId, processInstanceId);

        try {
            Object existingAssigneeList = execution.getVariable("assigneeList");
            if (existingAssigneeList != null) {
                log.debug("assigneeList已存在, 跳过注入, activityId={}, list={}", activityId, existingAssigneeList);
                return;
            }

            WfProcessVersionService processVersionService = getBean(WfProcessVersionService.class);
            WfNodeConfigService nodeConfigService = getBean(WfNodeConfigService.class);
            RepositoryService repositoryService = getBean(RepositoryService.class);

            ProcessDefinition processDef = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionId(processDefinitionId)
                    .singleResult();
            if (processDef == null) {
                log.warn("未找到流程定义, processDefId={}", processDefinitionId);
                return;
            }

            WfProcessVersion processVersion = processVersionService.getByFlowableProcessDefId(processDefinitionId);
            if (processVersion == null) {
                log.warn("未找到流程版本, processDefId={}", processDefinitionId);
                return;
            }

            WfNodeConfig nodeConfig = nodeConfigService.getByNodeId(processVersion.getId(), activityId);
            if (nodeConfig == null) {
                log.warn("未找到节点配置, processVersionId={}, nodeId={}", processVersion.getId(), activityId);
                return;
            }

            if (nodeConfig.getMultiInstance() == null || nodeConfig.getMultiInstance() != 1) {
                return;
            }

            Long startUserId = getStartUserId(execution);
            Long startDeptId = getStartDeptId(execution);

            AssigneeResolver resolver = getAssigneeResolver(nodeConfig.getAssigneeType());
            if (resolver == null) {
                log.error("不支持的审批人类型: {}, nodeId={}", nodeConfig.getAssigneeType(), activityId);
                return;
            }

            List<Long> assignees = resolver.resolve(nodeConfig, startUserId, startDeptId);
            if (assignees == null || assignees.isEmpty()) {
                log.warn("解析审批人为空, nodeId={}, assigneeType={}", activityId, nodeConfig.getAssigneeType());
                return;
            }

            List<String> assigneeList = assignees.stream()
                    .map(String::valueOf)
                    .toList();

            execution.setVariable("assigneeList", assigneeList);
            execution.setVariable("nrOfInstances", assigneeList.size());
            execution.setVariable("signApproveCount", 0);
            execution.setVariable("signRejectCount", 0);

            log.info("多实例审批人列表注入成功, activityId={}, size={}, assignees={}",
                    activityId, assigneeList.size(), assigneeList);

        } catch (Exception e) {
            log.error("多实例审批人列表注入失败, activityId={}, processDefId={}, error={}",
                    activityId, processDefinitionId, e.getMessage(), e);
        }
    }

    private Long getStartUserId(DelegateExecution execution) {
        Object val = execution.getVariable("startUserId");
        return toLong(val);
    }

    private Long getStartDeptId(DelegateExecution execution) {
        Object val = execution.getVariable("startDeptId");
        return toLong(val);
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Long) return (Long) val;
        if (val instanceof Number) return ((Number) val).longValue();
        if (val instanceof String) {
            try { return Long.parseLong((String) val); } catch (NumberFormatException e) { return null; }
        }
        return null;
    }

    private AssigneeResolver getAssigneeResolver(Integer assigneeType) {
        if (resolverCache.isEmpty()) {
            synchronized (MultiInstanceStartListener.class) {
                if (resolverCache.isEmpty()) {
                    try {
                        Map<String, AssigneeResolver> beans =
                                SpringContextHolder.getApplicationContext().getBeansOfType(AssigneeResolver.class);
                        for (AssigneeResolver resolver : beans.values()) {
                            resolverCache.put(resolver.getAssigneeType(), resolver);
                        }
                    } catch (Exception e) {
                        log.warn("获取审批人解析器失败: {}", e.getMessage());
                    }
                }
            }
        }
        return resolverCache.get(assigneeType);
    }

    private <T> T getBean(Class<T> clazz) {
        try {
            return SpringContextHolder.getApplicationContext().getBean(clazz);
        } catch (Exception e) {
            log.warn("获取Bean失败, type={}, error={}", clazz.getSimpleName(), e.getMessage());
            return null;
        }
    }
}
