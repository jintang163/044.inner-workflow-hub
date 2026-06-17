package com.innerworkflow.bpmn.dto;

import com.innerworkflow.common.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
public class WfBusinessLineQueryDTO extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    private String lineName;

    private String lineCode;

    private Integer status;
}
