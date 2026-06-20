package com.innerworkflow.approval.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class WfEscalationRuleSaveDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    @NotBlank(message = "规则名称不能为空")
    private String ruleName;

    @NotBlank(message = "规则编码不能为空")
    private String ruleCode;

    private String processKey;

    private String nodeId;

    @NotNull(message = "升级级别不能为空")
    private Integer escalateLevel;

    @NotNull(message = "超时时间不能为空")
    private Integer timeoutHours;

    @NotNull(message = "升级目标类型不能为空")
    private Integer escalateType;

    private String escalateTarget;

    @NotNull(message = "升级动作不能为空")
    private Integer escalateAction;

    private Integer enabled;

    private Integer sortOrder;
}
