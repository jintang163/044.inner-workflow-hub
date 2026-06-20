package com.innerworkflow.approval.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class WfCommentTemplateSaveDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    @NotNull(message = "分类ID不能为空")
    private Long categoryId;

    @NotBlank(message = "模板名称不能为空")
    private String templateName;

    @NotBlank(message = "模板内容不能为空")
    private String templateContent;

    @NotNull(message = "适用范围不能为空")
    private Integer scopeType;

    private Long deptId;

    private Integer sortOrder;

    private Integer status;

    private String remark;
}
