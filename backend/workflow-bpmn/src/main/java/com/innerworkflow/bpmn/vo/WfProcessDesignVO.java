package com.innerworkflow.bpmn.vo;

import com.innerworkflow.bpmn.entity.WfNodeConfig;
import com.innerworkflow.bpmn.entity.WfProcessDefinition;
import com.innerworkflow.bpmn.entity.WfProcessVersion;
import com.innerworkflow.bpmn.entity.WfSequenceFlowConfig;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
public class WfProcessDesignVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long processDefinitionId;

    private String processKey;

    private String processName;

    private WfProcessDefinition processDefinition;

    private WfProcessVersion currentVersion;

    private String bpmnXml;

    private List<WfNodeConfig> nodeConfigs;

    private List<WfSequenceFlowConfig> sequenceFlowConfigs;

    private Map<String, Object> globalNotifyConfig;

    private Map<String, Object> formSchema;
}
