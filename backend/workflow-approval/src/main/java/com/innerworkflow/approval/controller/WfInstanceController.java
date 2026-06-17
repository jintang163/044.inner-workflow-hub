package com.innerworkflow.approval.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.innerworkflow.approval.dto.WfMyProcessQueryDTO;
import com.innerworkflow.approval.service.WfApprovalService;
import com.innerworkflow.approval.service.WfProcessInstanceService;
import com.innerworkflow.approval.vo.WfProcessDetailVO;
import com.innerworkflow.approval.vo.WfProcessInstanceVO;
import com.innerworkflow.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "流程实例", description = "流程实例相关接口")
@RestController
@RequestMapping("/approval/instance")
@RequiredArgsConstructor
public class WfInstanceController {

    private final WfProcessInstanceService processInstanceService;
    private final WfApprovalService approvalService;

    @Operation(summary = "我发起的流程")
    @GetMapping("/my")
    public R<IPage<WfProcessInstanceVO>> myProcess(WfMyProcessQueryDTO queryDTO) {
        return R.success(processInstanceService.pageMyProcess(queryDTO));
    }

    @Operation(summary = "流程详情")
    @GetMapping("/{id}")
    public R<WfProcessDetailVO> getDetail(@PathVariable Long id) {
        return R.success(approvalService.getProcessDetail(id));
    }

    @Operation(summary = "流程图")
    @GetMapping("/{id}/diagram")
    public R<String> getDiagram(@PathVariable Long id) {
        return R.success(approvalService.getProcessDiagram(id));
    }

    @Operation(summary = "根据单号获取实例")
    @GetMapping("/no/{instanceNo}")
    public R<WfProcessInstanceVO> getByInstanceNo(@PathVariable String instanceNo) {
        return R.success(processInstanceService.getDetailById(
                processInstanceService.getByInstanceNo(instanceNo).getId()
        ));
    }
}
