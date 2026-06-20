package com.innerworkflow.approval.dto;

import com.innerworkflow.common.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
public class WfInstanceMigrationQueryDTO extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long processDefinitionId;

    private String processKey;

    private Long currentVersionId;

    private String title;

    private String instanceNo;

    private Long startUserId;

    private Integer instanceStatus;
}
