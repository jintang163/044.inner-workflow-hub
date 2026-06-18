package com.innerworkflow.common.util;

import com.innerworkflow.common.context.TenantContext;
import com.innerworkflow.common.dto.LoginUserDTO;
import com.innerworkflow.common.enums.ResultCode;
import com.innerworkflow.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * 安全工具类
 * <p>
 * 提供获取当前登录用户信息的能力，优先从Spring Security上下文获取，
 * 降级到ThreadLocal获取（用于非Web请求场景，如异步任务、消息消费等）
 * </p>
 *
 * @author InnerWorkflow Team
 * @since 1.0.0
 */
@Slf4j
public class SecurityUtils {

    /**
     * ThreadLocal存储登录用户信息（用于Spring Security未覆盖的场景）
     */
    private static final ThreadLocal<LoginUserDTO> USER_HOLDER = new ThreadLocal<>();

    /**
     * 获取当前登录用户
     * <p>
     * 未登录时抛出异常
     * </p>
     *
     * @throws BusinessException 未认证时抛出401异常
     */
    public static LoginUserDTO getCurrentUser() {
        LoginUserDTO user = getUserFromHolder();
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return user;
    }

    /**
     * 获取当前登录用户（Optional包装，避免NPE）
     */
    public static Optional<LoginUserDTO> getCurrentUserOpt() {
        return Optional.ofNullable(getUserFromHolder());
    }

    /**
     * 获取当前登录用户ID
     * <p>
     * 未登录时抛出异常
     * </p>
     */
    public static Long getCurrentUserId() {
        return getCurrentUser().getUserId();
    }

    /**
     * 获取当前登录用户ID（允许为null）
     */
    public static Long getCurrentUserIdOrNull() {
        LoginUserDTO user = getUserFromHolder();
        return user != null ? user.getUserId() : null;
    }

    /**
     * 获取当前登录用户名
     */
    public static String getCurrentUsername() {
        return getCurrentUser().getUsername();
    }

    /**
     * 获取当前登录用户真实姓名
     */
    public static String getCurrentRealName() {
        return getCurrentUser().getRealName();
    }

    /**
     * 获取当前登录用户部门ID
     */
    public static Long getCurrentDeptId() {
        return getCurrentUser().getDeptId();
    }

    /**
     * 判断是否已登录
     */
    public static boolean isLoggedIn() {
        return getUserFromHolder() != null;
    }

    /**
     * 判断当前用户是否拥有指定角色
     */
    public static boolean hasRole(String role) {
        LoginUserDTO user = getUserFromHolder();
        return user != null && user.hasRole(role);
    }

    /**
     * 判断当前用户是否拥有指定权限
     */
    public static boolean hasPermission(String permission) {
        LoginUserDTO user = getUserFromHolder();
        return user != null && user.hasPermission(permission);
    }

    public static Long getCurrentTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            return tenantId;
        }
        LoginUserDTO user = getUserFromHolder();
        return user != null ? user.getTenantId() : null;
    }

    public static boolean isSuperAdmin() {
        LoginUserDTO user = getUserFromHolder();
        return user != null && user.isSuperAdmin();
    }

    public static void setCurrentUser(LoginUserDTO user) {
        USER_HOLDER.set(user);
    }

    /**
     * 清除当前线程的登录用户（使用完毕后必须调用，防止内存泄漏）
     */
    public static void clearCurrentUser() {
        USER_HOLDER.remove();
    }

    /**
     * 从Holder中获取用户
     * <p>
     * 优先尝试从Spring Security上下文获取，如果未集成或获取失败则使用ThreadLocal
     * </p>
     */
    private static LoginUserDTO getUserFromHolder() {
        // 1. 尝试从ThreadLocal获取（适用于手动注入的场景）
        LoginUserDTO user = USER_HOLDER.get();
        if (user != null) {
            return user;
        }

        // 2. 尝试从Spring Security上下文获取（如果classpath中存在Security）
        try {
            Class<?> securityContextHolder = Class.forName("org.springframework.security.core.context.SecurityContextHolder");
            Object context = securityContextHolder.getMethod("getContext").invoke(null);
            if (context != null) {
                Object authentication = context.getClass().getMethod("getAuthentication").invoke(context);
                if (authentication != null) {
                    Object principal = authentication.getClass().getMethod("getPrincipal").invoke(authentication);
                    if (principal instanceof LoginUserDTO) {
                        return (LoginUserDTO) principal;
                    }
                }
            }
        } catch (Exception e) {
            // Spring Security未集成或获取失败，静默忽略
            log.trace("从Spring Security获取用户信息失败: {}", e.getMessage());
        }

        return null;
    }
}
