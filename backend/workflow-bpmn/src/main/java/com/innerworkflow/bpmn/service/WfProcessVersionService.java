package com.innerworkflow.bpmn.service;

import com.innerworkflow.bpmn.entity.WfProcessVersion;

import java.util.List;

public interface WfProcessVersionService {

    List<WfProcessVersion> listByProcessDefinitionId(Long processDefinitionId);

    WfProcessVersion getById(Long id);

    WfProcessVersion getCurrentVersion(Long processDefinitionId);

    WfProcessVersion getByVersion(Long processDefinitionId, Integer version);

    boolean save(WfProcessVersion processVersion);

    boolean updateById(WfProcessVersion processVersion);
}
