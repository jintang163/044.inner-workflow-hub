package com.innerworkflow.bpmn.handler;

import com.innerworkflow.bpmn.entity.WfNodeConfig;
import com.innerworkflow.common.enums.AssigneeTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class FixedUserAssigneeResolver implements AssigneeResolver {

    @Override
    public Integer getAssigneeType() {
        return AssigneeTypeEnum.FIXED_USER.getCode();
    }

    @Override
    public List<Long> resolve(WfNodeConfig nodeConfig, Long startUserId, Long startDeptId) {
        List<Long> assigneeValue = nodeConfig.getAssigneeValue();
        if (assigneeValue == null || assigneeValue.isEmpty()) {
            log.warn("固定人员审批人为空, nodeId={}", nodeConfig.getNodeId());
            return Collections.emptyList();
        }
        return new ArrayList<>(assigneeValue);
    }
}
