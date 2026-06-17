package com.innerworkflow.approval.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class WfApprovalHistoryVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long instanceId;

    private String nodeId;

    private String nodeName;

    private Integer activityType;

    private String activityTypeName;

    private Long operatorId;

    private String operatorName;

    private String operatorDeptName;

    private String operatorAvatar;

    private Long targetUserId;

    private String targetUserName;

    private String targetNodeId;

    private String targetNodeName;

    private String actionRemark;

    private String signatureUrl;

    private List<Long> attachmentIds;

    private Long duration;

    private Integer isValid;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime operateTime;
}
