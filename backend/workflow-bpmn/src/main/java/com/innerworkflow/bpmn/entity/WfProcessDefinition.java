package com.innerworkflow.bpmn.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.innerworkflow.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "wf_process_definition", autoResultMap = true)
public class WfProcessDefinition extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    private String processKey;

    private String processName;

    private Long businessLineId;

    private Long categoryId;

    private String description;

    private String icon;

    private Integer currentVersion;

    private Integer processStatus;

    private Long formId;

    private Integer startPermissionType;

    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private List<Long> startPermissionJson;

    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private List<Long> adminUserIds;

    private Integer sortOrder;
}
