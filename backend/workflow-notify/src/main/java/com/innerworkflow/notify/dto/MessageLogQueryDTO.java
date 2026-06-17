package com.innerworkflow.notify.dto;

import com.innerworkflow.common.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class MessageLogQueryDTO extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    private String templateCode;

    private String businessType;

    private Long instanceId;

    private Long taskId;

    private String channelType;

    private Long receiverUserId;

    private Integer sendStatus;

    private LocalDateTime startTime;

    private LocalDateTime endTime;
}
