package com.innerworkflow.notify.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

@Data
public class NotifySendDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String templateCode;

    private String eventType;

    private String businessType;

    private Long instanceId;

    private Long taskId;

    private Long receiverUserId;

    private String receiverAccount;

    private Map<String, Object> params;

    private Integer maxRetry;
}
