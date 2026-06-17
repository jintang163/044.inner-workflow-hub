package com.innerworkflow.bpmn.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class WfProcessVersionVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long processDefinitionId;

    private String processKey;

    private Integer version;

    private String flowableDeploymentId;

    private String flowableProcessDefId;

    private Long formId;

    private Integer formVersion;

    private String versionRemark;

    private Long publishBy;

    private String publishByName;

    private LocalDateTime publishTime;

    private Integer isCurrent;

    private Integer suspendStatus;

    private LocalDateTime createTime;
}
