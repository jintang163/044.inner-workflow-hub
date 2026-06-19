package com.innerworkflow.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.context.ApplicationEvent;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

public class ApprovalStatusUpdateEvent extends ApplicationEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final EventPayload payload;

    public ApprovalStatusUpdateEvent(Object source, EventPayload payload) {
        super(source);
        this.payload = payload;
    }

    public EventPayload getPayload() {
        return payload;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventPayload implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private Long instanceId;
        private String instanceNo;
        private Integer instanceStatus;
        private String instanceStatusName;
        private String actionType;
        private String actionTypeName;
        private Long operatorId;
        private String operatorName;
        private Integer version;
        private LocalDateTime timestamp;
    }
}
