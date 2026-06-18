package com.innerworkflow.bpmn.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@TableName(value = "wf_process_version", autoResultMap = true)
public class WfProcessVersion implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long processDefinitionId;

    private String processKey;

    private Integer version;

    private String flowableDeploymentId;

    private String flowableProcessDefId;

    private String bpmnXml;

    private Long formId;

    private Integer formVersion;

    private String versionRemark;

    private Long publishBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishTime;

    private Integer isCurrent;

    private Integer suspendStatus;

    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private Map<String, Object> globalNotifyConfig;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
