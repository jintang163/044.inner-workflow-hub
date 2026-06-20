package com.innerworkflow.approval.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.innerworkflow.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_comment_template_category")
public class WfCommentTemplateCategory extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    private String categoryName;

    private String categoryCode;

    private Integer scopeType;

    private Long deptId;

    private Integer sortOrder;

    private Integer status;

    private String remark;
}
