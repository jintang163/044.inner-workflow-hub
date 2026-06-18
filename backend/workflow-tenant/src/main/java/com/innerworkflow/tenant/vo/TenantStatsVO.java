package com.innerworkflow.tenant.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class TenantStatsVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long tenantId;

    private String tenantName;

    private Long processCount;

    private Long pendingCount;

    private Long avgDuration;
}
