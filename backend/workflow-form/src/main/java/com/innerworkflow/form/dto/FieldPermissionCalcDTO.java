package com.innerworkflow.form.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

@Data
public class FieldPermissionCalcDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "表单ID不能为空")
    private Long formId;

    @NotNull(message = "表单版本不能为空")
    private Integer formVersion;

    @NotBlank(message = "节点ID不能为空")
    private String nodeId;

    private Map<String, String> nodeFieldPermissions;

    private Map<String, Object> formData;
}
