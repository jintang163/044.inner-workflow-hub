package com.innerworkflow.form.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.innerworkflow.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "wf_form_draft", autoResultMap = true)
public class WfFormDraft extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    private String draftNo;

    private Long processDefinitionId;

    private String processKey;

    private Long formId;

    private Integer formVersion;

    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private Object formData;

    private Long creatorId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
