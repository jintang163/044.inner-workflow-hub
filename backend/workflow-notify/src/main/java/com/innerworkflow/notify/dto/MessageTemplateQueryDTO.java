package com.innerworkflow.notify.dto;

import com.innerworkflow.common.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
public class MessageTemplateQueryDTO extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    private String templateName;

    private String templateCode;

    private String eventType;

    private Long businessLineId;

    private Integer status;
}
