package com.innerworkflow.tenant.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.innerworkflow.common.result.R;
import com.innerworkflow.common.util.SecurityUtils;
import com.innerworkflow.tenant.dto.TenantQueryDTO;
import com.innerworkflow.tenant.dto.TenantRegisterDTO;
import com.innerworkflow.tenant.dto.TenantUpdateDTO;
import com.innerworkflow.tenant.dto.TenantUserQueryDTO;
import com.innerworkflow.tenant.service.SysTenantService;
import com.innerworkflow.tenant.vo.TenantStatsVO;
import com.innerworkflow.tenant.vo.TenantUserVO;
import com.innerworkflow.tenant.vo.TenantVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "租户管理")
@RestController
@RequestMapping("/tenant")
@RequiredArgsConstructor
public class SysTenantController {

    private final SysTenantService tenantService;

    @Operation(summary = "租户注册")
    @PostMapping("/register")
    public R<Void> register(@Valid @RequestBody TenantRegisterDTO registerDTO) {
        tenantService.register(registerDTO);
        return R.success();
    }

    @Operation(summary = "租户分页查询")
    @GetMapping("/page")
    public R<IPage<TenantVO>> page(TenantQueryDTO queryDTO) {
        return R.success(tenantService.page(queryDTO));
    }

    @Operation(summary = "租户详情")
    @GetMapping("/{id}")
    public R<TenantVO> getById(@PathVariable Long id) {
        return R.success(tenantService.getById(id));
    }

    @Operation(summary = "审批通过")
    @PutMapping("/approve/{id}")
    public R<Void> approve(@PathVariable Long id) {
        tenantService.approve(id);
        return R.success();
    }

    @Operation(summary = "审批驳回")
    @PutMapping("/reject/{id}")
    public R<Void> reject(@PathVariable Long id) {
        tenantService.reject(id);
        return R.success();
    }

    @Operation(summary = "更新租户")
    @PutMapping
    public R<Void> update(@Valid @RequestBody TenantUpdateDTO updateDTO) {
        tenantService.update(updateDTO);
        return R.success();
    }

    @Operation(summary = "删除租户")
    @DeleteMapping("/{id}")
    public R<Void> remove(@PathVariable Long id) {
        tenantService.remove(id);
        return R.success();
    }

    @Operation(summary = "当前用户所属租户列表")
    @GetMapping("/list-by-user")
    public R<List<TenantVO>> listByUser() {
        return R.success(tenantService.listByUserId(SecurityUtils.getCurrentUserId()));
    }

    @Operation(summary = "租户统计")
    @GetMapping("/{id}/stats")
    public R<TenantStatsVO> getStats(@PathVariable Long id) {
        return R.success(tenantService.getStats(id));
    }

    @Operation(summary = "租户用户分页查询")
    @GetMapping("/{id}/users")
    public R<IPage<TenantUserVO>> pageUsers(@PathVariable Long id, TenantUserQueryDTO queryDTO) {
        queryDTO.setTenantId(id);
        return R.success(tenantService.pageUsers(queryDTO));
    }

    @Operation(summary = "添加租户用户")
    @PostMapping("/{id}/users")
    public R<Void> addTenantUser(@PathVariable Long id,
                                 @RequestParam Long userId,
                                 @RequestParam String tenantRole) {
        tenantService.addTenantUser(id, userId, tenantRole);
        return R.success();
    }

    @Operation(summary = "移除租户用户")
    @DeleteMapping("/{id}/users/{userId}")
    public R<Void> removeTenantUser(@PathVariable Long id, @PathVariable Long userId) {
        tenantService.removeTenantUser(id, userId);
        return R.success();
    }
}
