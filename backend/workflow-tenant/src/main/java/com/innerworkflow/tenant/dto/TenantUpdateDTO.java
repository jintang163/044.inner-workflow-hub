package com.innerworkflow.tenant.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class TenantUpdateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "租户ID不能为空")
    private Long id;

    private String tenantName;

    private String contactName;

    private String contactEmail;

    private String contactPhone;

    private String businessType;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireTime;

    private String remark;

    private Integer status;
}
