package com.innerworkflow.bpmn.dto;

import com.innerworkflow.common.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
public class WfProcessDefinitionQueryDTO extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long businessLineId;

    private Long categoryId;

    private String processName;

    private String processKey;

    private Integer processStatus;
}
