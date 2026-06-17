package com.innerworkflow.form.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName(value = "wf_form_version", autoResultMap = true)
public class WfFormVersion implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long formDefinitionId;

    private String formKey;

    private Integer version;

    private String formSchema;

    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private Object fieldMapping;

    private String versionRemark;

    private Long publishBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishTime;

    private Integer isCurrent;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
