package com.innerworkflow.approval.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class WfCommentTemplateCategorySaveDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    @NotBlank(message = "分类名称不能为空")
    private String categoryName;

    @NotBlank(message = "分类编码不能为空")
    private String categoryCode;

    @NotNull(message = "适用范围不能为空")
    private Integer scopeType;

    private Long deptId;

    private Integer sortOrder;

    private Integer status;

    private String remark;
}
