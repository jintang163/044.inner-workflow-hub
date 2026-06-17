package com.innerworkflow.auth.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.innerworkflow.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
public class SysMenu extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableField("parent_id")
    private Long parentId;

    @TableField("menu_name")
    private String menuName;

    @TableField("path")
    private String path;

    @TableField("component")
    private String component;

    @TableField("permission")
    private String permission;

    @TableField("menu_type")
    private String menuType;

    @TableField("visible")
    private Integer visible;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("icon")
    private String icon;

    @TableField("status")
    private Integer status;

    @TableField(exist = false)
    private List<SysMenu> children;
}
