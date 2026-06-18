package com.innerworkflow.tenant.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class TenantRegisterDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "租户名称不能为空")
    private String tenantName;

    @NotBlank(message = "租户编码不能为空")
    private String tenantCode;

    @NotBlank(message = "联系人不能为空")
    private String contactName;

    @Email(message = "邮箱格式不正确")
    private String contactEmail;

    private String contactPhone;

    @NotBlank(message = "业务类型不能为空")
    private String businessType;

    private String remark;
}
