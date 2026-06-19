package com.innerworkflow.approval.dto;

import com.innerworkflow.common.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
public class WfDelegationQueryDTO extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    private Integer delegationStatus;

    private String delegatorName;

    private String delegateeName;

    private Integer queryType;
}
