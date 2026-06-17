package com.innerworkflow.auth.controller;

import com.innerworkflow.auth.dto.LoginDTO;
import com.innerworkflow.auth.dto.RegisterDTO;
import com.innerworkflow.auth.service.AuthService;
import com.innerworkflow.auth.service.SysMenuService;
import com.innerworkflow.auth.vo.LoginVO;
import com.innerworkflow.auth.vo.RouterVO;
import com.innerworkflow.auth.vo.UserInfoVO;
import com.innerworkflow.common.result.R;
import com.innerworkflow.common.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "认证管理")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SysMenuService sysMenuService;

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public R<LoginVO> login(@Valid @RequestBody LoginDTO loginDTO) {
        LoginVO result = authService.login(loginDTO);
        return R.success(result);
    }

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public R<Void> register(@Valid @RequestBody RegisterDTO registerDTO) {
        authService.register(registerDTO);
        return R.success();
    }

    @Operation(summary = "用户登出")
    @PostMapping("/logout")
    public R<Void> logout() {
        authService.logout();
        return R.success();
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/userInfo")
    public R<UserInfoVO> getUserInfo() {
        UserInfoVO userInfo = authService.getUserInfo();
        return R.success(userInfo);
    }

    @Operation(summary = "获取当前用户路由")
    @GetMapping("/routers")
    public R<List<RouterVO>> getRouters() {
        Long userId = SecurityUtils.getCurrentUserId();
        List<RouterVO> routers = sysMenuService.buildRouters(userId);
        return R.success(routers);
    }
}
