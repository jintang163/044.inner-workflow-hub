package com.innerworkflow.bpmn.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("wf_sequence_flow_config")
public class WfSequenceFlowConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long processVersionId;

    private String processKey;

    private String flowId;

    private String flowName;

    private String sourceNodeId;

    private String targetNodeId;

    private Integer conditionType;

    private String conditionExpression;

    private String conditionScript;

    private Integer conditionPriority;

    private Integer isDefault;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
