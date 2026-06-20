package com.innerworkflow.approval.dto;

import com.innerworkflow.common.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
public class WfCommentTemplateCategoryQueryDTO extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    private String categoryName;

    private String categoryCode;

    private Integer scopeType;

    private Integer status;
}
