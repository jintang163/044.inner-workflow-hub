package com.innerworkflow.auth.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.innerworkflow.auth.dto.RoleQueryDTO;
import com.innerworkflow.auth.dto.RoleSaveDTO;
import com.innerworkflow.auth.service.SysRoleService;
import com.innerworkflow.auth.vo.RoleVO;
import com.innerworkflow.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "角色管理")
@RestController
@RequestMapping("/api/system/role")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleService sysRoleService;

    @Operation(summary = "分页查询角色列表")
    @GetMapping("/page")
    public R<IPage<RoleVO>> getRolePage(@Valid RoleQueryDTO queryDTO) {
        IPage<RoleVO> page = sysRoleService.getRolePage(queryDTO);
        return R.success(page);
    }

    @Operation(summary = "获取全部角色列表")
    @GetMapping("/list")
    public R<List<RoleVO>> getRoleList() {
        List<RoleVO> list = sysRoleService.getRoleList();
        return R.success(list);
    }

    @Operation(summary = "根据ID获取角色详情")
    @GetMapping("/{id}")
    public R<RoleVO> getRoleById(@PathVariable Long id) {
        RoleVO role = sysRoleService.getRoleById(id);
        return R.success(role);
    }

    @Operation(summary = "新增角色")
    @PostMapping
    public R<Void> saveRole(@Valid @RequestBody RoleSaveDTO saveDTO) {
        sysRoleService.saveRole(saveDTO);
        return R.success();
    }

    @Operation(summary = "修改角色")
    @PutMapping
    public R<Void> updateRole(@Valid @RequestBody RoleSaveDTO saveDTO) {
        sysRoleService.updateRole(saveDTO);
        return R.success();
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/{id}")
    public R<Void> deleteRole(@PathVariable Long id) {
        sysRoleService.deleteRole(id);
        return R.success();
    }

    @Operation(summary = "批量删除角色")
    @DeleteMapping("/batch")
    public R<Void> batchDeleteRole(@RequestBody List<Long> ids) {
        sysRoleService.batchDeleteRole(ids);
        return R.success();
    }
}
