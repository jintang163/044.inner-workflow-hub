package com.innerworkflow.auth.controller;

import com.innerworkflow.auth.dto.MenuSaveDTO;
import com.innerworkflow.auth.service.SysMenuService;
import com.innerworkflow.auth.vo.MenuVO;
import com.innerworkflow.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "菜单管理")
@RestController
@RequestMapping("/api/system/menu")
@RequiredArgsConstructor
public class SysMenuController {

    private final SysMenuService sysMenuService;

    @Operation(summary = "获取菜单树")
    @GetMapping("/tree")
    public R<List<MenuVO>> getMenuTree() {
        List<MenuVO> tree = sysMenuService.getMenuTree();
        return R.success(tree);
    }

    @Operation(summary = "获取菜单列表")
    @GetMapping("/list")
    public R<List<MenuVO>> getMenuList(
            @RequestParam(required = false) String menuName,
            @RequestParam(required = false) Integer status) {
        List<MenuVO> list = sysMenuService.getMenuList(menuName, status);
        return R.success(list);
    }

    @Operation(summary = "根据ID获取菜单详情")
    @GetMapping("/{id}")
    public R<MenuVO> getMenuById(@PathVariable Long id) {
        MenuVO menu = sysMenuService.getMenuById(id);
        return R.success(menu);
    }

    @Operation(summary = "新增菜单")
    @PostMapping
    public R<Void> saveMenu(@Valid @RequestBody MenuSaveDTO saveDTO) {
        sysMenuService.saveMenu(saveDTO);
        return R.success();
    }

    @Operation(summary = "修改菜单")
    @PutMapping
    public R<Void> updateMenu(@Valid @RequestBody MenuSaveDTO saveDTO) {
        sysMenuService.updateMenu(saveDTO);
        return R.success();
    }

    @Operation(summary = "删除菜单")
    @DeleteMapping("/{id}")
    public R<Void> deleteMenu(@PathVariable Long id) {
        sysMenuService.deleteMenu(id);
        return R.success();
    }
}
