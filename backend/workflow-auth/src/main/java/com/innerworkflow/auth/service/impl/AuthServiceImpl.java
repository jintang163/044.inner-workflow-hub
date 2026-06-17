package com.innerworkflow.auth.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.innerworkflow.auth.dto.LoginDTO;
import com.innerworkflow.auth.dto.RegisterDTO;
import com.innerworkflow.auth.entity.SysUser;
import com.innerworkflow.auth.mapper.SysUserMapper;
import com.innerworkflow.auth.service.AuthService;
import com.innerworkflow.auth.service.SysMenuService;
import com.innerworkflow.auth.service.SysUserService;
import com.innerworkflow.auth.vo.LoginVO;
import com.innerworkflow.auth.vo.UserInfoVO;
import com.innerworkflow.common.dto.LoginUserDTO;
import com.innerworkflow.common.exception.BusinessException;
import com.innerworkflow.common.util.JwtUtils;
import com.innerworkflow.common.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements AuthService {

    private final SysUserService sysUserService;
    private final SysMenuService sysMenuService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public LoginVO login(LoginDTO loginDTO) {
        SysUser user = this.getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, loginDTO.getUsername()));
        if (user == null) {
            throw BusinessException.paramError("用户名或密码错误");
        }

        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw BusinessException.paramError("用户名或密码错误");
        }

        if (user.getStatus() == 0) {
            throw BusinessException.paramError("账号已被停用");
        }

        LoginUserDTO loginUser = sysUserService.getLoginUserByUsername(user.getUsername());
        if (loginUser == null) {
            throw BusinessException.paramError("用户信息加载失败");
        }

        Set<String> perms = Set.copyOf(sysMenuService.getPermsByUserId(user.getId()));
        loginUser.setPermissions(perms);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        String token = JwtUtils.generateToken(claims);
        loginUser.setToken(token);

        updateLoginInfo(user);

        SecurityUtils.setCurrentUser(loginUser);

        UserInfoVO userInfo = buildUserInfo(loginUser);

        return LoginVO.builder()
                .token(token)
                .userInfo(userInfo)
                .build();
    }

    @Override
    public void register(RegisterDTO registerDTO) {
        SysUser existsUser = this.getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, registerDTO.getUsername()));
        if (existsUser != null) {
            throw BusinessException.paramError("用户名已存在");
        }

        SysUser user = BeanUtil.copyProperties(registerDTO, SysUser.class);
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        if (user.getNickName() == null) {
            user.setNickName(registerDTO.getRealName());
        }
        user.setStatus(1);
        user.setUserType(1);

        this.save(user);
    }

    @Override
    public void logout() {
        SecurityUtils.clearCurrentUser();
    }

    @Override
    public UserInfoVO getUserInfo() {
        LoginUserDTO loginUser = SecurityUtils.getCurrentUser();

        Set<String> perms = Set.copyOf(sysMenuService.getPermsByUserId(loginUser.getUserId()));
        loginUser.setPermissions(perms);

        return buildUserInfo(loginUser);
    }

    private void updateLoginInfo(SysUser user) {
        HttpServletRequest request = getRequest();
        String ip = request != null ? getClientIp(request) : null;

        SysUser updateUser = new SysUser();
        updateUser.setId(user.getId());
        updateUser.setLoginIp(ip);
        updateUser.setLoginTime(LocalDateTime.now());
        this.updateById(updateUser);
    }

    private UserInfoVO buildUserInfo(LoginUserDTO loginUser) {
        SysUser user = this.getById(loginUser.getUserId());
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }

        return UserInfoVO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .nickName(user.getNickName())
                .realName(user.getRealName())
                .avatar(user.getAvatar())
                .gender(user.getGender())
                .email(user.getEmail())
                .phone(user.getPhone())
                .deptId(user.getDeptId())
                .deptName(loginUser.getDeptName())
                .roles(loginUser.getRoles())
                .permissions(loginUser.getPermissions())
                .createTime(user.getCreateTime())
                .build();
    }

    private HttpServletRequest getRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
