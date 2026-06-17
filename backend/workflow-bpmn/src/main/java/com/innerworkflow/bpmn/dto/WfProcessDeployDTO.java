package com.innerworkflow.bpmn.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class WfProcessDeployDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "流程定义ID不能为空")
    private Long processDefinitionId;

    private String versionRemark;

    private Long formId;

    private Integer formVersion;
}
