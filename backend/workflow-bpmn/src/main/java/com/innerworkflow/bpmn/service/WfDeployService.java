package com.innerworkflow.bpmn.service;

import com.innerworkflow.bpmn.dto.WfProcessDeployDTO;
import com.innerworkflow.bpmn.dto.WfProcessDesignSaveDTO;
import com.innerworkflow.bpmn.vo.WfProcessDesignVO;

public interface WfDeployService {

    WfProcessDesignVO getDesignData(Long processDefinitionId);

    void saveDesign(WfProcessDesignSaveDTO saveDTO);

    void deployProcess(WfProcessDeployDTO deployDTO);

    void suspendProcess(Long processVersionId);

    void activateProcess(Long processVersionId);

    String generateBpmnXml(Long processDefinitionId);
}
