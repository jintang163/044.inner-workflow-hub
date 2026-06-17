package com.innerworkflow.bpmn.handler;

import com.innerworkflow.bpmn.entity.WfNodeConfig;
import com.innerworkflow.common.enums.AssigneeTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class StartUserLeaderAssigneeResolver implements AssigneeResolver {

    @Override
    public Integer getAssigneeType() {
        return AssigneeTypeEnum.START_USER_LEADER.getCode();
    }

    @Override
    public List<Long> resolve(WfNodeConfig nodeConfig, Long startUserId, Long startDeptId) {
        if (startDeptId == null) {
            log.warn("发起人上级审批人解析失败: 发起人部门ID为空, nodeId={}", nodeConfig.getNodeId());
            return Collections.emptyList();
        }

        log.debug("发起人上级审批人解析, startDeptId={}, nodeId={}", startDeptId, nodeConfig.getNodeId());
        return Collections.emptyList();
    }
}
