package com.innerworkflow.approval.controller;

import com.innerworkflow.approval.dto.*;
import com.innerworkflow.approval.service.WfApprovalService;
import com.innerworkflow.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "审批操作", description = "审批操作相关接口")
@RestController
@RequestMapping("/approval")
@RequiredArgsConstructor
public class WfApprovalController {

    private final WfApprovalService approvalService;

    @Operation(summary = "发起审批")
    @PostMapping("/start")
    public R<String> startProcess(@Valid @RequestBody WfStartProcessDTO dto) {
        String instanceNo = approvalService.startProcess(dto);
        return R.success(instanceNo);
    }

    @Operation(summary = "同意")
    @PostMapping("/approve")
    public R<Void> approve(@Valid @RequestBody WfApprovalActionDTO dto) {
        approvalService.approve(dto);
        return R.success();
    }

    @Operation(summary = "拒绝")
    @PostMapping("/reject")
    public R<Void> reject(@Valid @RequestBody WfApprovalActionDTO dto) {
        approvalService.reject(dto);
        return R.success();
    }

    @Operation(summary = "转审")
    @PostMapping("/transfer")
    public R<Void> transfer(@Valid @RequestBody WfTransferDTO dto) {
        approvalService.transfer(dto);
        return R.success();
    }

    @Operation(summary = "加签")
    @PostMapping("/add-sign")
    public R<Void> addSign(@Valid @RequestBody WfAddSignDTO dto) {
        approvalService.addSign(dto);
        return R.success();
    }

    @Operation(summary = "委派")
    @PostMapping("/delegate")
    public R<Void> delegate(@Valid @RequestBody WfDelegateDTO dto) {
        approvalService.delegate(dto);
        return R.success();
    }

    @Operation(summary = "驳回")
    @PostMapping("/reject-to-node")
    public R<Void> rejectToNode(@Valid @RequestBody WfRejectDTO dto) {
        approvalService.rejectToNode(dto);
        return R.success();
    }

    @Operation(summary = "撤回")
    @PostMapping("/withdraw")
    public R<Void> withdraw(@Valid @RequestBody WfWithdrawDTO dto) {
        approvalService.withdraw(dto);
        return R.success();
    }

    @Operation(summary = "批量审批")
    @PostMapping("/batch")
    public R<Void> batchApprove(@Valid @RequestBody WfBatchApprovalDTO dto) {
        approvalService.batchApprove(dto);
        return R.success();
    }

    @Operation(summary = "批量转审")
    @PostMapping("/batch-transfer")
    public R<Void> batchTransfer(@Valid @RequestBody WfBatchTransferDTO dto) {
        approvalService.batchTransfer(dto);
        return R.success();
    }
}
