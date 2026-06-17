package com.innerworkflow.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class RoleSaveDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    @NotBlank(message = "角色名称不能为空")
    private String roleName;

    @NotBlank(message = "角色编码不能为空")
    private String roleCode;

    private Integer roleSort;

    private Integer dataScope;

    private Integer status;

    private String remark;

    private List<Long> menuIds;

    private List<Long> deptIds;
}
