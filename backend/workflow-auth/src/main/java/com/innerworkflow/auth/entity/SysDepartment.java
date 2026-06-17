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
@TableName("sys_department")
public class SysDepartment extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableField("parent_id")
    private Long parentId;

    @TableField("ancestors")
    private String ancestors;

    @TableField("dept_name")
    private String deptName;

    @TableField("dept_code")
    private String deptCode;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("leader_user_id")
    private Long leaderUserId;

    @TableField("phone")
    private String phone;

    @TableField("email")
    private String email;

    @TableField("status")
    private Integer status;

    @TableField(exist = false)
    private List<SysDepartment> children;
}
