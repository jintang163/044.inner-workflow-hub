package com.innerworkflow.auth.vo;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
public class UserInfoVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long userId;

    private String username;

    private String nickName;

    private String realName;

    private String avatar;

    private Integer gender;

    private String email;

    private String phone;

    private Long deptId;

    private String deptName;

    private Set<String> roles;

    private Set<String> permissions;

    private LocalDateTime createTime;
}
