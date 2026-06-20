package com.innerworkflow.approval.service;

import com.innerworkflow.approval.entity.WfApprovalTask;
import com.innerworkflow.approval.entity.WfEscalationRule;

import java.util.List;

public interface WfEscalationService {

    void processEscalation();

    void processTaskEscalation(WfApprovalTask task, List<WfEscalationRule> rules);

    boolean executeEscalation(WfApprovalTask task, WfEscalationRule rule);

    List<Long> resolveEscalateUsers(WfApprovalTask task, WfEscalationRule rule);
}
