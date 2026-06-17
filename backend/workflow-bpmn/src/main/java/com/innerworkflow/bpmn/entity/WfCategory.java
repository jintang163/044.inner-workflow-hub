package com.innerworkflow.bpmn.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.innerworkflow.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_category")
public class WfCategory extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long businessLineId;

    private String categoryName;

    private String categoryCode;

    private String categoryIcon;

    private String description;

    private Integer sortOrder;

    private Integer status;
}
