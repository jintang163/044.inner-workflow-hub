package com.innerworkflow.bpmn.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.innerworkflow.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_business_line")
public class WfBusinessLine extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    private String lineName;

    private String lineCode;

    private String lineIcon;

    private String description;

    private Integer sortOrder;

    private Integer status;
}
