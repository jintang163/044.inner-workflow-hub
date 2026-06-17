package com.innerworkflow.notify.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@TableName(value = "wf_message_log", autoResultMap = true)
public class WfMessageLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private String messageNo;

    private Long templateId;

    private String templateCode;

    private String businessType;

    private Long instanceId;

    private Long taskId;

    private String channelType;

    private String sender;

    private Long receiverUserId;

    private String receiverAccount;

    private String messageTitle;

    private String messageContent;

    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private Map<String, Object> messageParams;

    private Integer sendStatus;

    private Integer retryCount;

    private Integer maxRetry;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime sendTime;

    private String failReason;

    private String thirdPartyMsgId;

    private Integer isRead;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
