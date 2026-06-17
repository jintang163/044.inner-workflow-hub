package com.innerworkflow.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Set;

/**
 * 当前登录用户信息DTO
 * <p>
 * 存储在Security上下文或ThreadLocal中的用户信息
 * </p>
 *
 * @author InnerWorkflow Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginUserDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名（登录账号）
     */
    private String username;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 所属部门ID
     */
    private Long deptId;

    /**
     * 所属部门名称
     */
    private String deptName;

    /**
     * 角色编码集合
     */
    private Set<String> roles;

    /**
     * 权限标识集合
     */
    private Set<String> permissions;

    /**
     * Token值
     */
    private String token;

    /**
     * 判断是否拥有指定角色
     */
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    /**
     * 判断是否拥有指定权限
     */
    public boolean hasPermission(String permission) {
        return permissions != null && permissions.contains(permission);
    }
}
