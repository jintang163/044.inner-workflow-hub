package com.innerworkflow.approval.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("wf_attachment_permission")
public class WfAttachmentPermission implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long processVersionId;

    private String nodeId;

    private String nodeName;

    private Integer attachmentVisible;

    private Integer attachmentEditable;

    private Long maxFileSize;

    private String allowedTypes;

    private Integer maxFileCount;

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private Long tenantId;

    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private Long createBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @TableLogic
    @TableField(value = "is_deleted", fill = FieldFill.INSERT)
    @JsonIgnore
    private Integer isDeleted;
}
