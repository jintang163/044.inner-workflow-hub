package com.innerworkflow.bpmn.service;

import com.innerworkflow.bpmn.entity.WfNodeConfig;

import java.util.List;

public interface WfNodeConfigService {

    List<WfNodeConfig> listByProcessVersionId(Long processVersionId);

    WfNodeConfig getByNodeId(Long processVersionId, String nodeId);

    boolean saveBatch(List<WfNodeConfig> nodeConfigs);

    boolean removeByProcessVersionId(Long processVersionId);
}
