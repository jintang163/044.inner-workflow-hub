package com.innerworkflow.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class UserSaveDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度在3-50个字符之间")
    private String username;

    private String password;

    @NotBlank(message = "真实姓名不能为空")
    private String realName;

    private String nickName;

    private String email;

    private String phone;

    private String avatar;

    private Integer gender;

    private Long deptId;

    private Integer status;

    private List<Long> roleIds;
}
