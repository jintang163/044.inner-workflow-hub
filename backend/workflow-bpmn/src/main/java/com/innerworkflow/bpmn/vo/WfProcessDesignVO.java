package com.innerworkflow.bpmn.vo;

import com.innerworkflow.bpmn.entity.WfNodeConfig;
import com.innerworkflow.bpmn.entity.WfSequenceFlowConfig;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class WfProcessDesignVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long processDefinitionId;

    private String processKey;

    private String processName;

    private String bpmnXml;

    private List<WfNodeConfig> nodeConfigs;

    private List<WfSequenceFlowConfig> sequenceFlowConfigs;
}
