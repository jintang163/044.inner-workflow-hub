package com.innerworkflow.tenant.dto;

import com.innerworkflow.common.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
public class TenantUserQueryDTO extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long tenantId;

    private String username;

    private String tenantRole;
}
