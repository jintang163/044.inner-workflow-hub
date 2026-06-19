package com.innerworkflow.approval.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class WfTransferRecordVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long instanceId;

    private String instanceNo;

    private Long taskId;

    private String flowableTaskId;

    private String nodeId;

    private String nodeName;

    private Integer transferType;

    private String transferTypeName;

    private Long sourceUserId;

    private String sourceUserName;

    private Long targetUserId;

    private String targetUserName;

    private String transferReason;

    private Long delegationId;

    private String title;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
