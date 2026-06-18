package com.innerworkflow.approval.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName(value = "wf_approval_task", autoResultMap = true)
public class WfApprovalTask implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String taskNo;

    private Long instanceId;

    private String flowableTaskId;

    private String flowableExecutionId;

    private String processKey;

    private String nodeId;

    private String nodeName;

    private Integer nodeType;

    private Integer approveType;

    private Integer multiInstanceFlag;

    private Long assigneeId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime assignTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dueTime;

    private Integer action;

    private String actionRemark;

    private Long actionUserId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime actionTime;

    private Long actionDuration;

    private String signatureUrl;

    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private List<Long> attachmentIds;

    private Integer taskStatus;

    private Integer sourceType;

    private Long sourceTaskId;

    private Integer escalateLevel;

    private Long tenantId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
