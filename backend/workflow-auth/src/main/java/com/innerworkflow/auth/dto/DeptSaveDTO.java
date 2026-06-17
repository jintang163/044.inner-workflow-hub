package com.innerworkflow.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class DeptSaveDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long parentId;

    @NotBlank(message = "部门名称不能为空")
    private String deptName;

    @NotBlank(message = "部门编码不能为空")
    private String deptCode;

    private Integer sortOrder;

    private Long leaderUserId;

    private String phone;

    private String email;

    private Integer status;
}
