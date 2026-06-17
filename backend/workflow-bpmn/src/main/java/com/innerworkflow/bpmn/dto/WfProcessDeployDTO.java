package com.innerworkflow.bpmn.dto;

import com.innerworkflow.bpmn.entity.WfNodeConfig;
import com.innerworkflow.bpmn.entity.WfSequenceFlowConfig;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class WfProcessDeployDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "流程定义ID不能为空")
    private Long processDefinitionId;

    @NotBlank(message = "流程Key不能为空")
    private String processKey;

    private String versionRemark;

    private Long formId;

    private Integer formVersion;

    private List<WfNodeConfig> nodeConfigs;

    private List<WfSequenceFlowConfig> sequenceFlowConfigs;
}
