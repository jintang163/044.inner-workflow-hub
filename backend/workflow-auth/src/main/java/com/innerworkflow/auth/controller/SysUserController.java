package com.innerworkflow.auth.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.innerworkflow.auth.dto.UserQueryDTO;
import com.innerworkflow.auth.dto.UserSaveDTO;
import com.innerworkflow.auth.service.SysUserService;
import com.innerworkflow.auth.vo.UserVO;
import com.innerworkflow.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "用户管理")
@RestController
@RequestMapping("/api/system/user")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService sysUserService;

    @Operation(summary = "分页查询用户列表")
    @GetMapping("/page")
    public R<IPage<UserVO>> getUserPage(@Valid UserQueryDTO queryDTO) {
        IPage<UserVO> page = sysUserService.getUserPage(queryDTO);
        return R.success(page);
    }

    @Operation(summary = "根据ID获取用户详情")
    @GetMapping("/{id}")
    public R<UserVO> getUserById(@PathVariable Long id) {
        UserVO user = sysUserService.getUserById(id);
        return R.success(user);
    }

    @Operation(summary = "新增用户")
    @PostMapping
    public R<Void> saveUser(@Valid @RequestBody UserSaveDTO saveDTO) {
        sysUserService.saveUser(saveDTO);
        return R.success();
    }

    @Operation(summary = "修改用户")
    @PutMapping
    public R<Void> updateUser(@Valid @RequestBody UserSaveDTO saveDTO) {
        sysUserService.updateUser(saveDTO);
        return R.success();
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    public R<Void> deleteUser(@PathVariable Long id) {
        sysUserService.deleteUser(id);
        return R.success();
    }

    @Operation(summary = "批量删除用户")
    @DeleteMapping("/batch")
    public R<Void> batchDeleteUser(@RequestBody List<Long> ids) {
        sysUserService.batchDeleteUser(ids);
        return R.success();
    }
}
