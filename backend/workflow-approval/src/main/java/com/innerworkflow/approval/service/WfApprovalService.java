package com.innerworkflow.approval.service;

import com.innerworkflow.approval.dto.*;
import com.innerworkflow.approval.vo.WfProcessDetailVO;

public interface WfApprovalService {

    String startProcess(WfStartProcessDTO dto);

    void approve(WfApprovalActionDTO dto);

    void reject(WfApprovalActionDTO dto);

    void transfer(WfTransferDTO dto);

    void addSign(WfAddSignDTO dto);

    void delegate(WfDelegateDTO dto);

    void rejectToNode(WfRejectDTO dto);

    void withdraw(WfWithdrawDTO dto);

    void batchApprove(WfBatchApprovalDTO dto);

    WfProcessDetailVO getProcessDetail(Long instanceId);

    String getProcessDiagram(Long instanceId);
}
