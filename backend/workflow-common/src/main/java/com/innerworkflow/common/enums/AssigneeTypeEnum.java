package com.innerworkflow.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 审批人类型枚举
 *
 * @author InnerWorkflow Team
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum AssigneeTypeEnum {

    /**
     * 指定人员
     */
    FIXED_USER(1, "指定人员"),

    /**
     * 部门负责人
     */
    DEPT_LEADER(2, "部门负责人"),

    /**
     * 指定角色
     */
    ROLE(3, "指定角色"),

    /**
     * 发起人自己
     */
    START_USER(4, "发起人"),

    /**
     * 发起人的上级
     */
    START_USER_LEADER(5, "发起人的上级"),

    /**
     * 脚本动态计算
     */
    SCRIPT(6, "脚本计算");

    /**
     * 类型码
     */
    private final Integer code;

    /**
     * 类型描述
     */
    private final String desc;

    /**
     * 根据类型码获取枚举
     */
    public static AssigneeTypeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (AssigneeTypeEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
