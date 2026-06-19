package com.innerworkflow.approval.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.innerworkflow.common.entity.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "wf_process_instance", autoResultMap = true)
public class WfProcessInstance extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    private String instanceNo;

    private Long processDefinitionId;

    private String processKey;

    private Long processVersionId;

    private String flowableProcessInstId;

    private String flowableProcessDefId;

    private Long businessLineId;

    private Long categoryId;

    private String title;

    private Long formId;

    private Integer formVersion;

    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private Object formData;

    private Integer instanceStatus;

    private Long startUserId;

    private String startUserName;

    private String startUserAvatar;

    private Long startDeptId;

    private String startDeptName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    private Long duration;

    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private List<String> currentNodeIds;

    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private List<Long> currentApproverIds;

    private Integer priority;

    private Integer rejectCount;

    private Integer maxRejectCount;

    private Integer formDataVersion;
}
