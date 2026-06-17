package com.innerworkflow.notify.event;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

@Data
public class NotifyEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String eventType;

    private String businessType;

    private Long instanceId;

    private Long taskId;

    private Long receiverUserId;

    private String receiverAccount;

    private String templateCode;

    private Map<String, Object> params;
}
