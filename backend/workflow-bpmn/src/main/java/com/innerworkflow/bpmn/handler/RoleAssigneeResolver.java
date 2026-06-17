package com.innerworkflow.bpmn.handler;

import com.innerworkflow.bpmn.entity.WfNodeConfig;
import com.innerworkflow.common.enums.AssigneeTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class RoleAssigneeResolver implements AssigneeResolver {

    @Override
    public Integer getAssigneeType() {
        return AssigneeTypeEnum.ROLE.getCode();
    }

    @Override
    public List<Long> resolve(WfNodeConfig nodeConfig, Long startUserId, Long startDeptId) {
        List<Long> roleIds = nodeConfig.getAssigneeValue();
        if (roleIds == null || roleIds.isEmpty()) {
            log.warn("角色审批人配置为空, nodeId={}", nodeConfig.getNodeId());
            return Collections.emptyList();
        }

        log.debug("角色审批人解析, roleIds={}, nodeId={}", roleIds, nodeConfig.getNodeId());
        return Collections.emptyList();
    }
}
