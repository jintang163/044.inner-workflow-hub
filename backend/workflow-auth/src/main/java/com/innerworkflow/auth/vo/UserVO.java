package com.innerworkflow.auth.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String username;

    private String nickName;

    private String realName;

    private String avatar;

    private Integer gender;

    private String email;

    private String phone;

    private Long deptId;

    private String deptName;

    private Integer status;

    private List<Long> roleIds;

    private List<String> roleNames;

    private LocalDateTime createTime;
}
