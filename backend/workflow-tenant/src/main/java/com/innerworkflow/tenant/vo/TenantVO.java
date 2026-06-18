package com.innerworkflow.tenant.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class TenantVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String tenantName;

    private String tenantCode;

    private String contactName;

    private String contactEmail;

    private String contactPhone;

    private String businessType;

    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireTime;

    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    private Long userCount;
}
