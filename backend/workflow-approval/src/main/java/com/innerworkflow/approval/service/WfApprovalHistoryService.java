package com.innerworkflow.approval.service;

import com.innerworkflow.approval.entity.WfApprovalHistory;

import java.util.List;

public interface WfApprovalHistoryService {

    List<WfApprovalHistory> listByInstanceId(Long instanceId);

    List<WfApprovalHistory> listValidByInstanceId(Long instanceId);

    boolean save(WfApprovalHistory history);

    boolean saveBatch(List<WfApprovalHistory> list);

    void markInvalidByInstanceIdAndNodeId(Long instanceId, String nodeId);
}
