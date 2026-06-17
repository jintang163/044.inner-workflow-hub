package com.innerworkflow.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class RegisterDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度在3-50个字符之间")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度在6-100个字符之间")
    private String password;

    @NotBlank(message = "真实姓名不能为空")
    private String realName;

    private String nickName;

    @Email(message = "邮箱格式不正确")
    private String email;

    private String phone;

    private Long deptId;
}
