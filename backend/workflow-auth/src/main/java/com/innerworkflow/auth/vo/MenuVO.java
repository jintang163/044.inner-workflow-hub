package com.innerworkflow.auth.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class MenuVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long parentId;

    private String menuName;

    private String path;

    private String component;

    private String permission;

    private String menuType;

    private Integer visible;

    private Integer sortOrder;

    private String icon;

    private Integer status;

    private LocalDateTime createTime;

    private List<MenuVO> children;
}
