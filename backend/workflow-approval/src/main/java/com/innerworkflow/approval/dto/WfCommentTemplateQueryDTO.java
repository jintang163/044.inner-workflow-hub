package com.innerworkflow.approval.dto;

import com.innerworkflow.common.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
public class WfCommentTemplateQueryDTO extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long categoryId;

    private String templateName;

    private String keyword;

    private Integer scopeType;

    private Integer status;

    private String sortBy;
}
