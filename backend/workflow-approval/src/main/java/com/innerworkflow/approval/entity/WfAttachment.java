package com.innerworkflow.approval.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("wf_attachment")
public class WfAttachment implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String fileName;

    private String fileSuffix;

    private Long fileSize;

    private String fileType;

    private Integer storageType;

    private String storagePath;

    private String accessUrl;

    private String md5;

    private Long uploadUserId;

    private String bizType;

    private String bizId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @TableLogic
    @TableField(value = "is_deleted", fill = FieldFill.INSERT)
    @JsonIgnore
    private Integer isDeleted;
}
