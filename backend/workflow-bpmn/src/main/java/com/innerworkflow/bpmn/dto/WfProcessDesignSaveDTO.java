package com.innerworkflow.bpmn.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
public class WfProcessDesignSaveDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "流程定义ID不能为空")
    private Long processDefinitionId;

    @NotBlank(message = "BPMN XML不能为空")
    private String bpmnXml;

    private List<WfNodeConfigDTO> nodeConfigs;

    private List<WfSequenceFlowConfigDTO> sequenceFlowConfigs;

    @Data
    public static class WfNodeConfigDTO implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @NotBlank(message = "节点ID不能为空")
        private String nodeId;

        @NotBlank(message = "节点名称不能为空")
        private String nodeName;

        @NotBlank(message = "节点类型不能为空")
        private String nodeType;

        private Integer approveType;

        private Integer multiInstance;

        private Integer assigneeType;

        private List<Long> assigneeValue;

        private String assigneeScript;

        private Map<String, String> formPermission;

        private Integer timeoutStrategy;

        private Integer timeoutHours;

        private Integer timeoutEscalateLevels;

        private Map<String, Object> notifyConfig;

        private Integer emptyAssigneeStrategy;

        private Integer refuseStrategy;

        private String refuseTargetNodeId;

        private Integer canAddSign;

        private Integer canTransfer;

        private Integer canDelegate;

        private Integer needSignature;

        private Integer needComment;

        private Integer sortOrder;
    }

    @Data
    public static class WfSequenceFlowConfigDTO implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @NotBlank(message = "连线ID不能为空")
        private String flowId;

        private String flowName;

        @NotBlank(message = "源节点ID不能为空")
        private String sourceNodeId;

        @NotBlank(message = "目标节点ID不能为空")
        private String targetNodeId;

        private Integer conditionType;

        private String conditionExpression;

        private String conditionScript;

        private Integer conditionPriority;

        private Integer isDefault;
    }
}
