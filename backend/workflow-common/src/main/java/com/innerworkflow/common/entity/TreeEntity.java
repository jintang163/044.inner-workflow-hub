package com.innerworkflow.common.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

/**
 * 树形结构基础实体类
 * <p>
 * 继承自 {@link BaseEntity}，增加树形结构所需的字段：父级ID、祖先链、排序号
 * </p>
 *
 * @author InnerWorkflow Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TreeEntity<T> extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 父级ID（顶级节点为0）
     */
    @TableField("parent_id")
    private Long parentId;

    /**
     * 祖先链（格式：/0/1/2/，便于查询所有子孙节点）
     */
    @TableField("ancestors")
    private String ancestors;

    /**
     * 排序号（数值越小越靠前）
     */
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * 子节点列表（非数据库字段，用于树形组装）
     */
    @TableField(exist = false)
    private List<T> children = new ArrayList<>();
}
