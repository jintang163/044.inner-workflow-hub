package com.innerworkflow.approval.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.innerworkflow.approval.dto.*;
import com.innerworkflow.approval.service.WfApprovalService;
import com.innerworkflow.approval.service.WfTransferRecordService;
import com.innerworkflow.approval.vo.WfTransferRecordVO;
import com.innerworkflow.common.dto.PageQuery;
import com.innerworkflow.common.result.R;
import com.innerworkflow.common.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "审批操作", description = "审批操作相关接口")
@RestController
@RequestMapping("/approval")
@RequiredArgsConstructor
public class WfApprovalController {

    private final WfApprovalService approvalService;
    private final WfTransferRecordService transferRecordService;

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

    @Operation(summary = "转审记录分页查询")
    @GetMapping("/transfer-record/page")
    public R<IPage<WfTransferRecordVO>> transferRecordPage(PageQuery queryDTO,
                                                            @RequestParam(required = false) Integer transferType) {
        Long userId = SecurityUtils.getCurrentUserId();
        IPage<WfTransferRecordVO> page = transferRecordService.page(queryDTO, userId, transferType);
        return R.success(page);
    }

    @Operation(summary = "根据实例ID查询转审记录")
    @GetMapping("/transfer-record/instance/{instanceId}")
    public R<List<WfTransferRecordVO>> listTransferRecordByInstanceId(@PathVariable Long instanceId) {
        List<WfTransferRecordVO> list = transferRecordService.listVOByInstanceId(instanceId);
        return R.success(list);
    }
}
