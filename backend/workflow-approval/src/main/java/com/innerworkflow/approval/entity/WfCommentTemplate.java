package com.innerworkflow.approval.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.innerworkflow.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_comment_template")
public class WfCommentTemplate extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long categoryId;

    private String templateName;

    private String templateContent;

    private Integer scopeType;

    private Long deptId;

    private Integer sortOrder;

    private Integer useCount;

    private Integer status;

    private String remark;
}
