package com.innerworkflow.tenant.dto;

import com.innerworkflow.common.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
public class TenantQueryDTO extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    private String tenantName;

    private String tenantCode;

    private Integer status;

    private String businessType;
}
