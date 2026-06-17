package com.innerworkflow.auth.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class RoleVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String roleName;

    private String roleCode;

    private Integer roleSort;

    private Integer dataScope;

    private Integer status;

    private String remark;

    private List<Long> menuIds;

    private List<Long> deptIds;

    private LocalDateTime createTime;
}
