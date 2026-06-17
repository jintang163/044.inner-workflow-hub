package com.innerworkflow.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 流程实例状态枚举
 *
 * @author InnerWorkflow Team
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum InstanceStatusEnum {

    /**
     * 审批中
     */
    APPROVING(0, "审批中"),

    /**
     * 已通过
     */
    APPROVED(1, "已通过"),

    /**
     * 已拒绝
     */
    REJECTED(2, "已拒绝"),

    /**
     * 已撤销
     */
    CANCELED(3, "已撤销"),

    /**
     * 已挂起
     */
    SUSPENDED(4, "已挂起");

    /**
     * 状态码
     */
    private final Integer code;

    /**
     * 状态描述
     */
    private final String desc;

    /**
     * 根据状态码获取枚举
     */
    public static InstanceStatusEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (InstanceStatusEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
