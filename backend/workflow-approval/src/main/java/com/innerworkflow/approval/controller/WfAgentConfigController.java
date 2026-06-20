package com.innerworkflow.approval.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.innerworkflow.approval.dto.WfAgentConfigQueryDTO;
import com.innerworkflow.approval.dto.WfAgentConfigSaveDTO;
import com.innerworkflow.approval.entity.WfAgentConfig;
import com.innerworkflow.approval.service.WfAgentConfigService;
import com.innerworkflow.common.result.R;
import com.innerworkflow.common.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "代理人配置", description = "代理人配置相关接口")
@RestController
@RequestMapping("/approval/agent-config")
@RequiredArgsConstructor
public class WfAgentConfigController {

    private final WfAgentConfigService agentConfigService;

    @Operation(summary = "分页查询代理人配置")
    @GetMapping("/page")
    public R<IPage<WfAgentConfig>> page(WfAgentConfigQueryDTO queryDTO) {
        return R.success(agentConfigService.page(queryDTO));
    }

    @Operation(summary = "根据ID查询代理人配置")
    @GetMapping("/{id}")
    public R<WfAgentConfig> getById(@PathVariable Long id) {
        return R.success(agentConfigService.getById(id));
    }

    @Operation(summary = "查询我的代理人配置")
    @GetMapping("/my/list")
    public R<List<WfAgentConfig>> getMyAgentConfigs() {
        Long userId = SecurityUtils.getCurrentUserId();
        return R.success(agentConfigService.listByUserId(userId));
    }

    @Operation(summary = "查询用户的代理人配置")
    @GetMapping("/user/{userId}")
    public R<List<WfAgentConfig>> getAgentConfigsByUserId(@PathVariable Long userId) {
        return R.success(agentConfigService.listByUserId(userId));
    }

    @Operation(summary = "查询用户的默认代理人")
    @GetMapping("/default/{userId}")
    public R<WfAgentConfig> getDefaultAgent(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") Integer configType,
            @RequestParam(required = false) String processKey) {
        return R.success(agentConfigService.getDefaultAgent(userId, configType, processKey));
    }

    @Operation(summary = "新增代理人配置")
    @PostMapping
    public R<Void> save(@Valid @RequestBody WfAgentConfigSaveDTO dto) {
        agentConfigService.save(dto);
        return R.success();
    }

    @Operation(summary = "修改代理人配置")
    @PutMapping
    public R<Void> update(@Valid @RequestBody WfAgentConfigSaveDTO dto) {
        agentConfigService.update(dto);
        return R.success();
    }

    @Operation(summary = "删除代理人配置")
    @DeleteMapping("/{id}")
    public R<Void> deleteById(@PathVariable Long id) {
        agentConfigService.deleteById(id);
        return R.success();
    }
}
