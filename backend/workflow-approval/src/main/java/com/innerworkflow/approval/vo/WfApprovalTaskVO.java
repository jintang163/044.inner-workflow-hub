package com.innerworkflow.approval.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class WfApprovalTaskVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String taskNo;

    private Long instanceId;

    private String instanceNo;

    private String flowableTaskId;

    private String processKey;

    private String processName;

    private String nodeId;

    private String nodeName;

    private Integer nodeType;

    private String nodeTypeName;

    private Integer approveType;

    private String approveTypeName;

    private Integer multiInstanceFlag;

    private Long assigneeId;

    private String assigneeName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime assignTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dueTime;

    private Integer action;

    private String actionName;

    private String actionRemark;

    private Long actionUserId;

    private String actionUserName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime actionTime;

    private Long actionDuration;

    private String signatureUrl;

    private Integer taskStatus;

    private String taskStatusName;

    private Integer sourceType;

    private String sourceTypeName;

    private Long sourceTaskId;

    private Integer escalateLevel;

    private String title;

    private Long startUserId;

    private String startUserName;

    private Long startDeptId;

    private String startDeptName;

    private Integer priority;

    private String priorityName;

    private String businessLineName;

    private String categoryName;
}
