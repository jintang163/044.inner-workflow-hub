package com.innerworkflow.api.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalStatusUpdateVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String type;

    private Long instanceId;

    private String instanceNo;

    private Integer instanceStatus;

    private String instanceStatusName;

    private String actionType;

    private String actionTypeName;

    private Long operatorId;

    private String operatorName;

    private Integer version;

    private String timestamp;
}
