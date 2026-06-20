package com.innerworkflow.approval.dto;

import com.innerworkflow.common.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
public class WfAgentConfigQueryDTO extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long userId;

    private Long agentUserId;

    private Integer configType;

    private Integer enabled;
}
