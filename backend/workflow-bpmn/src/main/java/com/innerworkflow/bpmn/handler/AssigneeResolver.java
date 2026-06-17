package com.innerworkflow.bpmn.handler;

import com.innerworkflow.bpmn.entity.WfNodeConfig;

import java.util.List;

public interface AssigneeResolver {

    Integer getAssigneeType();

    List<Long> resolve(WfNodeConfig nodeConfig, Long startUserId, Long startDeptId);
}
