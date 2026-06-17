package com.innerworkflow.bpmn.handler;

import com.innerworkflow.bpmn.entity.WfNodeConfig;
import com.innerworkflow.common.enums.AssigneeTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class StartUserAssigneeResolver implements AssigneeResolver {

    @Override
    public Integer getAssigneeType() {
        return AssigneeTypeEnum.START_USER.getCode();
    }

    @Override
    public List<Long> resolve(WfNodeConfig nodeConfig, Long startUserId, Long startDeptId) {
        if (startUserId == null) {
            log.warn("发起人审批人解析失败: 发起人ID为空, nodeId={}", nodeConfig.getNodeId());
            return Collections.emptyList();
        }
        return Collections.singletonList(startUserId);
    }
}
