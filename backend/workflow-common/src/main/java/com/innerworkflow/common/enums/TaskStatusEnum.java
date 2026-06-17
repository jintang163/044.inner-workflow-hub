package com.innerworkflow.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 任务状态枚举
 *
 * @author InnerWorkflow Team
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum TaskStatusEnum {

    /**
     * 待办
     */
    PENDING(0, "待办"),

    /**
     * 已办
     */
    DONE(1, "已办"),

    /**
     * 已取消
     */
    CANCELED(2, "已取消"),

    /**
     * 已转审
     */
    TRANSFERRED(3, "已转审"),

    /**
     * 已委派
     */
    DELEGATED(4, "已委派"),

    /**
     * 已超时
     */
    TIMEOUT(5, "已超时");

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
    public static TaskStatusEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (TaskStatusEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
