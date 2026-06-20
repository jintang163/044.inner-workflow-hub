package com.innerworkflow.approval.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class WfCommentTemplateVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long categoryId;

    private String categoryName;

    private String templateName;

    private String templateContent;

    private Integer scopeType;

    private String scopeTypeName;

    private Long deptId;

    private String deptName;

    private Integer sortOrder;

    private Integer useCount;

    private Integer status;

    private String remark;

    private Long createBy;

    private String createByName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
