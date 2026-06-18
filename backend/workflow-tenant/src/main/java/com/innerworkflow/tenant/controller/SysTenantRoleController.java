package com.innerworkflow.tenant.controller;

import com.innerworkflow.common.result.R;
import com.innerworkflow.tenant.dto.TenantRoleSaveDTO;
import com.innerworkflow.tenant.service.SysTenantRoleService;
import com.innerworkflow.tenant.vo.TenantRoleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "租户角色管理")
@RestController
@RequestMapping("/tenant/role")
@RequiredArgsConstructor
public class SysTenantRoleController {

    private final SysTenantRoleService tenantRoleService;

    @Operation(summary = "租户角色列表")
    @GetMapping("/list")
    public R<List<TenantRoleVO>> list(@RequestParam Long tenantId) {
        return R.success(tenantRoleService.listByTenantId(tenantId));
    }

    @Operation(summary = "角色详情")
    @GetMapping("/{id}")
    public R<TenantRoleVO> getById(@PathVariable Long id) {
        return R.success(tenantRoleService.getById(id));
    }

    @Operation(summary = "新增角色")
    @PostMapping
    public R<Void> save(@Valid @RequestBody TenantRoleSaveDTO dto) {
        tenantRoleService.save(dto);
        return R.success();
    }

    @Operation(summary = "更新角色")
    @PutMapping
    public R<Void> update(@Valid @RequestBody TenantRoleSaveDTO dto) {
        tenantRoleService.update(dto);
        return R.success();
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/{id}")
    public R<Void> remove(@PathVariable Long id) {
        tenantRoleService.remove(id);
        return R.success();
    }

    @Operation(summary = "分配用户角色")
    @PostMapping("/assign-user")
    public R<Void> assignUserRole(@RequestParam Long tenantId,
                                  @RequestParam Long userId,
                                  @RequestParam Long tenantRoleId) {
        tenantRoleService.assignUserRole(tenantId, userId, tenantRoleId);
        return R.success();
    }

    @Operation(summary = "移除用户角色")
    @DeleteMapping("/remove-user")
    public R<Void> removeUserRole(@RequestParam Long tenantId,
                                  @RequestParam Long userId,
                                  @RequestParam Long tenantRoleId) {
        tenantRoleService.removeUserRole(tenantId, userId, tenantRoleId);
        return R.success();
    }
}
