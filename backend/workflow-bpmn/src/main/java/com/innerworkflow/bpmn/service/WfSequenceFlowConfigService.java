package com.innerworkflow.bpmn.service;

import com.innerworkflow.bpmn.entity.WfSequenceFlowConfig;

import java.util.List;

public interface WfSequenceFlowConfigService {

    List<WfSequenceFlowConfig> listByProcessVersionId(Long processVersionId);

    List<WfSequenceFlowConfig> listBySourceNodeId(Long processVersionId, String sourceNodeId);

    boolean saveBatch(List<WfSequenceFlowConfig> sequenceFlowConfigs);

    boolean removeByProcessVersionId(Long processVersionId);
}
