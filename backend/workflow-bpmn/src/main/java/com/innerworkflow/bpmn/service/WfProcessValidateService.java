package com.innerworkflow.bpmn.service;

import com.innerworkflow.bpmn.vo.WfProcessValidateResultVO;

public interface WfProcessValidateService {

    WfProcessValidateResultVO validateProcess(Long processDefinitionId);

    WfProcessValidateResultVO validateBpmnXml(String bpmnXml);
}
