package com.innerworkflow.approval.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.innerworkflow.approval.dto.WfEscalationRuleQueryDTO;
import com.innerworkflow.approval.dto.WfEscalationRuleSaveDTO;
import com.innerworkflow.approval.entity.WfEscalationRule;
import com.innerworkflow.approval.service.WfEscalationRuleService;
import com.innerworkflow.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "升级规则管理", description = "升级规则配置相关接口")
@RestController
@RequestMapping("/approval/escalation-rule")
@RequiredArgsConstructor
public class WfEscalationRuleController {

    private final WfEscalationRuleService escalationRuleService;

    @Operation(summary = "分页查询升级规则")
    @GetMapping("/page")
    public R<IPage<WfEscalationRule>> page(WfEscalationRuleQueryDTO queryDTO) {
        return R.success(escalationRuleService.page(queryDTO));
    }

    @Operation(summary = "根据ID查询升级规则")
    @GetMapping("/{id}")
    public R<WfEscalationRule> getById(@PathVariable Long id) {
        return R.success(escalationRuleService.getById(id));
    }

    @Operation(summary = "新增升级规则")
    @PostMapping
    public R<Void> save(@Valid @RequestBody WfEscalationRuleSaveDTO dto) {
        escalationRuleService.save(dto);
        return R.success();
    }

    @Operation(summary = "修改升级规则")
    @PutMapping
    public R<Void> update(@Valid @RequestBody WfEscalationRuleSaveDTO dto) {
        escalationRuleService.update(dto);
        return R.success();
    }

    @Operation(summary = "删除升级规则")
    @DeleteMapping("/{id}")
    public R<Void> deleteById(@PathVariable Long id) {
        escalationRuleService.deleteById(id);
        return R.success();
    }

    @Operation(summary = "查询流程节点的所有升级规则")
    @GetMapping("/list")
    public R<List<WfEscalationRule>> listByProcessAndNode(
            @RequestParam(required = false) String processKey,
            @RequestParam(required = false) String nodeId) {
        return R.success(escalationRuleService.listByProcessAndNode(processKey, nodeId));
    }

    @Operation(summary = "查询流程节点的启用升级规则")
    @GetMapping("/enabled")
    public R<List<WfEscalationRule>> listEnabledRules(
            @RequestParam(required = false) String processKey,
            @RequestParam(required = false) String nodeId) {
        return R.success(escalationRuleService.listEnabledRules(processKey, nodeId));
    }
}
