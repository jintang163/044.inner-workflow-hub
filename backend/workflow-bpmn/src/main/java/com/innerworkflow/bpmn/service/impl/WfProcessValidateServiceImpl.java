package com.innerworkflow.bpmn.service.impl;

import com.innerworkflow.bpmn.entity.WfNodeConfig;
import com.innerworkflow.bpmn.entity.WfProcessDefinition;
import com.innerworkflow.bpmn.entity.WfProcessVersion;
import com.innerworkflow.bpmn.entity.WfSequenceFlowConfig;
import com.innerworkflow.bpmn.service.WfNodeConfigService;
import com.innerworkflow.bpmn.service.WfProcessDefinitionService;
import com.innerworkflow.bpmn.service.WfProcessValidateService;
import com.innerworkflow.bpmn.service.WfProcessVersionService;
import com.innerworkflow.bpmn.service.WfSequenceFlowConfigService;
import com.innerworkflow.bpmn.vo.WfProcessValidateResultVO;
import com.innerworkflow.common.enums.ResultCode;
import com.innerworkflow.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WfProcessValidateServiceImpl implements WfProcessValidateService {

    private final WfProcessDefinitionService processDefinitionService;
    private final WfProcessVersionService processVersionService;
    private final WfNodeConfigService nodeConfigService;
    private final WfSequenceFlowConfigService sequenceFlowConfigService;

    private static final String LEVEL_ERROR = "ERROR";
    private static final String LEVEL_WARNING = "WARNING";

    @Override
    public WfProcessValidateResultVO validateProcess(Long processDefinitionId) {
        WfProcessDefinition processDefinition = processDefinitionService.getById(processDefinitionId);
        if (processDefinition == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "流程定义不存在");
        }

        WfProcessValidateResultVO result = new WfProcessValidateResultVO();
        result.setValid(true);

        WfProcessVersion currentVersion = processVersionService.getCurrentVersion(processDefinitionId);
        if (currentVersion == null) {
            currentVersion = processVersionService.getByVersion(processDefinitionId, 0);
        }

        if (currentVersion == null) {
            result.addError(LEVEL_ERROR, null, "流程尚未设计");
            result.setValid(false);
            return result;
        }

        List<WfNodeConfig> nodeConfigs = nodeConfigService.listByProcessVersionId(currentVersion.getId());
        List<WfSequenceFlowConfig> flowConfigs = sequenceFlowConfigService.listByProcessVersionId(currentVersion.getId());

        validateStartNode(nodeConfigs, result);
        validateEndNode(nodeConfigs, result);
        validateIsolatedNodes(nodeConfigs, flowConfigs, result);
        validateGatewayBranches(nodeConfigs, flowConfigs, result);
        validateUserTaskAssignee(nodeConfigs, result);
        validateSequenceFlow(nodeConfigs, flowConfigs, result);

        return result;
    }

    @Override
    public WfProcessValidateResultVO validateBpmnXml(String bpmnXml) {
        WfProcessValidateResultVO result = new WfProcessValidateResultVO();
        result.setValid(true);

        if (bpmnXml == null || bpmnXml.isEmpty()) {
            result.addError(LEVEL_ERROR, null, "BPMN XML不能为空");
            result.setValid(false);
            return result;
        }

        try {
            if (!bpmnXml.contains("<bpmn:definitions") && !bpmnXml.contains("<definitions")) {
                result.addError(LEVEL_ERROR, null, "BPMN XML格式不正确，缺少definitions根节点");
                result.setValid(false);
            }
            if (!bpmnXml.contains("<bpmn:process") && !bpmnXml.contains("<process")) {
                result.addError(LEVEL_ERROR, null, "BPMN XML中缺少process节点");
                result.setValid(false);
            }
        } catch (Exception e) {
            result.addError(LEVEL_ERROR, null, "BPMN XML解析失败: " + e.getMessage());
            result.setValid(false);
        }

        return result;
    }

    private void validateStartNode(List<WfNodeConfig> nodeConfigs, WfProcessValidateResultVO result) {
        List<WfNodeConfig> startNodes = nodeConfigs.stream()
                .filter(n -> "startEvent".equalsIgnoreCase(n.getNodeType())
                        || "START_EVENT".equalsIgnoreCase(n.getNodeType()))
                .collect(Collectors.toList());

        if (startNodes.isEmpty()) {
            result.addError(LEVEL_ERROR, null, "流程必须包含一个开始节点");
            result.setValid(false);
        } else if (startNodes.size() > 1) {
            result.addError(LEVEL_ERROR, null, "流程只能有一个开始节点");
            result.setValid(false);
        }
    }

    private void validateEndNode(List<WfNodeConfig> nodeConfigs, WfProcessValidateResultVO result) {
        List<WfNodeConfig> endNodes = nodeConfigs.stream()
                .filter(n -> "endEvent".equalsIgnoreCase(n.getNodeType())
                        || "END_EVENT".equalsIgnoreCase(n.getNodeType()))
                .collect(Collectors.toList());

        if (endNodes.isEmpty()) {
            result.addError(LEVEL_ERROR, null, "流程必须包含至少一个结束节点");
            result.setValid(false);
        }
    }

    private void validateIsolatedNodes(List<WfNodeConfig> nodeConfigs,
                                       List<WfSequenceFlowConfig> flowConfigs,
                                       WfProcessValidateResultVO result) {
        Set<String> nodeIds = nodeConfigs.stream()
                .map(WfNodeConfig::getNodeId)
                .collect(Collectors.toSet());

        Set<String> sourceNodeIds = flowConfigs.stream()
                .map(WfSequenceFlowConfig::getSourceNodeId)
                .collect(Collectors.toSet());

        Set<String> targetNodeIds = flowConfigs.stream()
                .map(WfSequenceFlowConfig::getTargetNodeId)
                .collect(Collectors.toSet());

        for (WfNodeConfig node : nodeConfigs) {
            String nodeType = node.getNodeType();
            String nodeId = node.getNodeId();

            if ("startEvent".equalsIgnoreCase(nodeType) || "START_EVENT".equalsIgnoreCase(nodeType)) {
                if (!sourceNodeIds.contains(nodeId)) {
                    result.addError(LEVEL_ERROR, nodeId, "开始节点没有流出连线");
                    result.setValid(false);
                }
            } else if ("endEvent".equalsIgnoreCase(nodeType) || "END_EVENT".equalsIgnoreCase(nodeType)) {
                if (!targetNodeIds.contains(nodeId)) {
                    result.addError(LEVEL_ERROR, nodeId, "结束节点没有流入连线");
                    result.setValid(false);
                }
            } else {
                if (!sourceNodeIds.contains(nodeId) && !targetNodeIds.contains(nodeId)) {
                    result.addError(LEVEL_WARNING, nodeId, "节点\"" + node.getNodeName() + "\"是孤立节点");
                } else if (!sourceNodeIds.contains(nodeId)) {
                    result.addError(LEVEL_ERROR, nodeId, "节点\"" + node.getNodeName() + "\"没有流出连线");
                    result.setValid(false);
                } else if (!targetNodeIds.contains(nodeId)) {
                    result.addError(LEVEL_ERROR, nodeId, "节点\"" + node.getNodeName() + "\"没有流入连线");
                    result.setValid(false);
                }
            }
        }
    }

    private void validateGatewayBranches(List<WfNodeConfig> nodeConfigs,
                                         List<WfSequenceFlowConfig> flowConfigs,
                                         WfProcessValidateResultVO result) {
        List<WfNodeConfig> gatewayNodes = nodeConfigs.stream()
                .filter(n -> "exclusiveGateway".equalsIgnoreCase(n.getNodeType())
                        || "EXCLUSIVE_GATEWAY".equalsIgnoreCase(n.getNodeType())
                        || "parallelGateway".equalsIgnoreCase(n.getNodeType())
                        || "PARALLEL_GATEWAY".equalsIgnoreCase(n.getNodeType()))
                .collect(Collectors.toList());

        for (WfNodeConfig gateway : gatewayNodes) {
            String nodeId = gateway.getNodeId();
            List<WfSequenceFlowConfig> outgoingFlows = flowConfigs.stream()
                    .filter(f -> nodeId.equals(f.getSourceNodeId()))
                    .collect(Collectors.toList());

            if (outgoingFlows.size() < 2) {
                result.addError(LEVEL_ERROR, nodeId, "网关节点\"" + gateway.getNodeName() + "\"至少需要2条流出连线");
                result.setValid(false);
            }

            if ("exclusiveGateway".equalsIgnoreCase(gateway.getNodeType())
                    || "EXCLUSIVE_GATEWAY".equalsIgnoreCase(gateway.getNodeType())) {
                boolean hasDefault = outgoingFlows.stream()
                        .anyMatch(f -> f.getIsDefault() != null && f.getIsDefault() == 1);

                if (!hasDefault && outgoingFlows.size() > 1) {
                    result.addError(LEVEL_WARNING, nodeId, "排他网关\"" + gateway.getNodeName() + "\"建议设置默认分支");
                }

                for (WfSequenceFlowConfig flow : outgoingFlows) {
                    if (flow.getIsDefault() == null || flow.getIsDefault() == 0) {
                        if (flow.getConditionExpression() == null || flow.getConditionExpression().isEmpty()) {
                            result.addError(LEVEL_ERROR, flow.getFlowId(),
                                    "连线\"" + (flow.getFlowName() != null ? flow.getFlowName() : flow.getFlowId())
                                            + "\"缺少条件表达式");
                            result.setValid(false);
                        }
                    }
                }
            }
        }
    }

    private void validateUserTaskAssignee(List<WfNodeConfig> nodeConfigs, WfProcessValidateResultVO result) {
        List<WfNodeConfig> userTasks = nodeConfigs.stream()
                .filter(n -> "userTask".equalsIgnoreCase(n.getNodeType())
                        || "USER_TASK".equalsIgnoreCase(n.getNodeType()))
                .collect(Collectors.toList());

        for (WfNodeConfig task : userTasks) {
            if (task.getAssigneeType() == null) {
                result.addError(LEVEL_ERROR, task.getNodeId(),
                        "审批节点\"" + task.getNodeName() + "\"必须设置审批人类型");
                result.setValid(false);
            } else {
                switch (task.getAssigneeType()) {
                    case 1:
                        if (task.getAssigneeValue() == null || task.getAssigneeValue().isEmpty()) {
                            result.addError(LEVEL_ERROR, task.getNodeId(),
                                    "审批节点\"" + task.getNodeName() + "\"必须指定审批人员");
                            result.setValid(false);
                        }
                        break;
                    case 2:
                    case 4:
                    case 5:
                        break;
                    case 3:
                        if (task.getAssigneeValue() == null || task.getAssigneeValue().isEmpty()) {
                            result.addError(LEVEL_ERROR, task.getNodeId(),
                                    "审批节点\"" + task.getNodeName() + "\"必须指定审批角色");
                            result.setValid(false);
                        }
                        break;
                    case 6:
                        if (task.getAssigneeScript() == null || task.getAssigneeScript().isEmpty()) {
                            result.addError(LEVEL_ERROR, task.getNodeId(),
                                    "审批节点\"" + task.getNodeName() + "\"必须指定审批人脚本");
                            result.setValid(false);
                        }
                        break;
                    default:
                        break;
                }
            }

            if (task.getMultiInstance() != null && task.getMultiInstance() == 1) {
                if (task.getApproveType() == null) {
                    result.addError(LEVEL_ERROR, task.getNodeId(),
                            "多实例节点\"" + task.getNodeName() + "\"必须设置审批类型（会签/或签/依次审批）");
                    result.setValid(false);
                }
            }

            if (task.getTimeoutStrategy() != null && task.getTimeoutStrategy() > 0) {
                if (task.getTimeoutHours() == null || task.getTimeoutHours() <= 0) {
                    result.addError(LEVEL_WARNING, task.getNodeId(),
                            "节点\"" + task.getNodeName() + "\"设置了超时策略但未设置超时时间");
                }
            }
        }
    }

    private void validateSequenceFlow(List<WfNodeConfig> nodeConfigs,
                                      List<WfSequenceFlowConfig> flowConfigs,
                                      WfProcessValidateResultVO result) {
        Set<String> nodeIds = nodeConfigs.stream()
                .map(WfNodeConfig::getNodeId)
                .collect(Collectors.toSet());

        for (WfSequenceFlowConfig flow : flowConfigs) {
            if (!nodeIds.contains(flow.getSourceNodeId())) {
                result.addError(LEVEL_ERROR, flow.getFlowId(),
                        "连线\"" + (flow.getFlowName() != null ? flow.getFlowName() : flow.getFlowId())
                                + "\"的源节点不存在");
                result.setValid(false);
            }
            if (!nodeIds.contains(flow.getTargetNodeId())) {
                result.addError(LEVEL_ERROR, flow.getFlowId(),
                        "连线\"" + (flow.getFlowName() != null ? flow.getFlowName() : flow.getFlowId())
                                + "\"的目标节点不存在");
                result.setValid(false);
            }
        }
    }
}
