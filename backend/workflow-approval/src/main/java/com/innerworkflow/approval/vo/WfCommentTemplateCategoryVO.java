package com.innerworkflow.approval.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class WfCommentTemplateCategoryVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String categoryName;

    private String categoryCode;

    private Integer scopeType;

    private String scopeTypeName;

    private Long deptId;

    private String deptName;

    private Integer sortOrder;

    private Integer status;

    private String remark;

    private Long createBy;

    private String createByName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
