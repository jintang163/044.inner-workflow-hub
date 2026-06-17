package com.innerworkflow.form.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class FormDefinitionSaveDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    @NotBlank(message = "表单标识不能为空")
    private String formKey;

    @NotBlank(message = "表单名称不能为空")
    private String formName;

    @NotNull(message = "业务线ID不能为空")
    private Long businessLineId;

    private String description;
}
