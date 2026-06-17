package com.innerworkflow.form.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class FormPublishDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "表单定义ID不能为空")
    private Long formDefinitionId;

    @NotBlank(message = "表单Schema不能为空")
    private String formSchema;

    private Object fieldMapping;

    private String versionRemark;
}
