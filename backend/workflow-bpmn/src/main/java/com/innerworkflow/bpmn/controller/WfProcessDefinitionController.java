package com.innerworkflow.bpmn.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.innerworkflow.bpmn.dto.WfProcessDefinitionQueryDTO;
import com.innerworkflow.bpmn.entity.WfProcessDefinition;
import com.innerworkflow.bpmn.entity.WfProcessVersion;
import com.innerworkflow.bpmn.service.WfProcessDefinitionService;
import com.innerworkflow.bpmn.service.WfProcessVersionService;
import com.innerworkflow.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "流程定义管理", description = "流程定义相关接口")
@RestController
@RequestMapping("/bpmn/process-definition")
@RequiredArgsConstructor
public class WfProcessDefinitionController {

    private final WfProcessDefinitionService processDefinitionService;
    private final WfProcessVersionService processVersionService;

    @Operation(summary = "分页查询流程定义")
    @GetMapping("/page")
    public R<IPage<WfProcessDefinition>> page(WfProcessDefinitionQueryDTO queryDTO) {
        return R.success(processDefinitionService.page(queryDTO));
    }

    @Operation(summary = "根据ID获取流程定义详情")
    @GetMapping("/{id}")
    public R<WfProcessDefinition> getById(@PathVariable Long id) {
        return R.success(processDefinitionService.getById(id));
    }

    @Operation(summary = "根据流程标识获取流程定义")
    @GetMapping("/key/{processKey}")
    public R<WfProcessDefinition> getByProcessKey(@PathVariable String processKey) {
        return R.success(processDefinitionService.getByProcessKey(processKey));
    }

    @Operation(summary = "新增流程定义")
    @PostMapping
    public R<Void> save(@RequestBody WfProcessDefinition processDefinition) {
        processDefinitionService.save(processDefinition);
        return R.success();
    }

    @Operation(summary = "修改流程定义")
    @PutMapping
    public R<Void> update(@RequestBody WfProcessDefinition processDefinition) {
        processDefinitionService.updateById(processDefinition);
        return R.success();
    }

    @Operation(summary = "删除流程定义")
    @DeleteMapping("/{id}")
    public R<Void> remove(@PathVariable Long id) {
        processDefinitionService.removeById(id);
        return R.success();
    }

    @Operation(summary = "获取流程版本列表")
    @GetMapping("/{processDefinitionId}/versions")
    public R<List<WfProcessVersion>> listVersions(@PathVariable Long processDefinitionId) {
        return R.success(processVersionService.listByProcessDefinitionId(processDefinitionId));
    }

    @Operation(summary = "获取当前版本详情")
    @GetMapping("/{processDefinitionId}/current-version")
    public R<WfProcessVersion> getCurrentVersion(@PathVariable Long processDefinitionId) {
        return R.success(processVersionService.getCurrentVersion(processDefinitionId));
    }
}
