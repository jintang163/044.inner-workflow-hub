package com.innerworkflow.bpmn.handler;

import com.innerworkflow.bpmn.entity.WfNodeConfig;
import com.innerworkflow.common.enums.AssigneeTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class DeptLeaderAssigneeResolver implements AssigneeResolver {

    @Override
    public Integer getAssigneeType() {
        return AssigneeTypeEnum.DEPT_LEADER.getCode();
    }

    @Override
    public List<Long> resolve(WfNodeConfig nodeConfig, Long startUserId, Long startDeptId) {
        if (startDeptId == null) {
            log.warn("部门负责人审批人解析失败: 发起人部门ID为空, nodeId={}", nodeConfig.getNodeId());
            return Collections.emptyList();
        }

        log.debug("部门负责人审批人解析, startDeptId={}, nodeId={}", startDeptId, nodeConfig.getNodeId());
        return Collections.emptyList();
    }
}
