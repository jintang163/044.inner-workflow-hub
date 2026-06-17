package com.innerworkflow.bpmn.controller;

import com.innerworkflow.bpmn.dto.WfProcessDeployDTO;
import com.innerworkflow.bpmn.dto.WfProcessDesignSaveDTO;
import com.innerworkflow.bpmn.service.WfDeployService;
import com.innerworkflow.bpmn.service.WfProcessValidateService;
import com.innerworkflow.bpmn.vo.WfProcessDesignVO;
import com.innerworkflow.bpmn.vo.WfProcessValidateResultVO;
import com.innerworkflow.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "流程设计器", description = "流程设计和部署相关接口")
@RestController
@RequestMapping("/bpmn/process-design")
@RequiredArgsConstructor
public class WfProcessDesignController {

    private final WfDeployService deployService;
    private final WfProcessValidateService processValidateService;

    @Operation(summary = "获取流程设计数据")
    @GetMapping("/{processDefinitionId}")
    public R<WfProcessDesignVO> getDesignData(@PathVariable Long processDefinitionId) {
        return R.success(deployService.getDesignData(processDefinitionId));
    }

    @Operation(summary = "保存流程设计")
    @PostMapping("/save")
    public R<Void> saveDesign(@Valid @RequestBody WfProcessDesignSaveDTO saveDTO) {
        deployService.saveDesign(saveDTO);
        return R.success();
    }

    @Operation(summary = "部署流程（发布新版本）")
    @PostMapping("/deploy")
    public R<Void> deployProcess(@Valid @RequestBody WfProcessDeployDTO deployDTO) {
        deployService.deployProcess(deployDTO);
        return R.success();
    }

    @Operation(summary = "校验流程")
    @GetMapping("/validate/{processDefinitionId}")
    public R<WfProcessValidateResultVO> validateProcess(@PathVariable Long processDefinitionId) {
        return R.success(processValidateService.validateProcess(processDefinitionId));
    }

    @Operation(summary = "校验BPMN XML")
    @PostMapping("/validate-xml")
    public R<WfProcessValidateResultVO> validateBpmnXml(@RequestBody String bpmnXml) {
        return R.success(processValidateService.validateBpmnXml(bpmnXml));
    }

    @Operation(summary = "挂起流程版本")
    @PostMapping("/suspend/{processVersionId}")
    public R<Void> suspendProcess(@PathVariable Long processVersionId) {
        deployService.suspendProcess(processVersionId);
        return R.success();
    }

    @Operation(summary = "激活流程版本")
    @PostMapping("/activate/{processVersionId}")
    public R<Void> activateProcess(@PathVariable Long processVersionId) {
        deployService.activateProcess(processVersionId);
        return R.success();
    }

    @Operation(summary = "获取BPMN XML")
    @GetMapping("/xml/{processDefinitionId}")
    public R<String> getBpmnXml(@PathVariable Long processDefinitionId) {
        return R.success(deployService.generateBpmnXml(processDefinitionId));
    }
}
