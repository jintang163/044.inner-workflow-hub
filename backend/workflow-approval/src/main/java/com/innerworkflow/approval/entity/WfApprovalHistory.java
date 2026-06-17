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
@TableName(value = "wf_approval_history", autoResultMap = true)
public class WfApprovalHistory implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long instanceId;

    private String flowableActInstId;

    private String nodeId;

    private String nodeName;

    private Integer activityType;

    private Long operatorId;

    private String operatorName;

    private String operatorDeptName;

    private Long targetUserId;

    private String targetUserName;

    private String targetNodeId;

    private String targetNodeName;

    private String actionRemark;

    private String signatureUrl;

    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private List<Long> attachmentIds;

    private Long duration;

    private Integer isValid;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime operateTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
