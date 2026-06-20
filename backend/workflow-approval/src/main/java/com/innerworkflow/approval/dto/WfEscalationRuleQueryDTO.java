package com.innerworkflow.approval.dto;

import com.innerworkflow.common.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
public class WfEscalationRuleQueryDTO extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    private String ruleName;

    private String ruleCode;

    private String processKey;

    private String nodeId;

    private Integer escalateLevel;

    private Integer escalateType;

    private Integer enabled;
}
