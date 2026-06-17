package com.innerworkflow.approval.dto;

import com.innerworkflow.common.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
public class WfMyProcessQueryDTO extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    private String processKey;

    private Long businessLineId;

    private Long categoryId;

    private String title;

    private Integer instanceStatus;
}
