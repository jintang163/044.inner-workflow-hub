package com.innerworkflow.form.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class FormDefinitionVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String formKey;

    private String formName;

    private Long businessLineId;

    private String description;

    private Integer currentVersion;

    private Integer status;

    private Long createBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
