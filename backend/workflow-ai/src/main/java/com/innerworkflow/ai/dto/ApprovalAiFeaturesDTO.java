package com.innerworkflow.ai.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class ApprovalAiFeaturesDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long instanceId;

    private BigDecimal amount;

    private Long departmentId;

    private Long initiatorId;

    private Integer initiatorLevel;

    private Long approverId;

    private String processKey;

    private Long businessLineId;

    private Integer priority;

    private Object formData;
}
