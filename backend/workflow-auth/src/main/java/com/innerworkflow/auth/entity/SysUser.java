package com.innerworkflow.auth.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.innerworkflow.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableField("username")
    private String username;

    @TableField("password")
    private String password;

    @TableField("nick_name")
    private String nickName;

    @TableField("real_name")
    private String realName;

    @TableField("user_type")
    private Integer userType;

    @TableField("email")
    private String email;

    @TableField("phone")
    private String phone;

    @TableField("avatar")
    private String avatar;

    @TableField("gender")
    private Integer gender;

    @TableField("dept_id")
    private Long deptId;

    @TableField("ding_user_id")
    private String dingUserId;

    @TableField("wecom_user_id")
    private String wecomUserId;

    @TableField("status")
    private Integer status;

    @TableField("login_ip")
    private String loginIp;

    @TableField("login_time")
    private LocalDateTime loginTime;

    @TableField("pwd_update_time")
    private LocalDateTime pwdUpdateTime;
}
