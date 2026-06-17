package com.innerworkflow.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class MenuSaveDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long parentId;

    @NotBlank(message = "菜单名称不能为空")
    private String menuName;

    private String path;

    private String component;

    private String permission;

    private String menuType;

    private Integer visible;

    private Integer sortOrder;

    private String icon;

    private Integer status;
}
