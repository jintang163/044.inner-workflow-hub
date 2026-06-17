package com.innerworkflow.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 流程定义状态枚举
 *
 * @author InnerWorkflow Team
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum ProcessStatusEnum {

    /**
     * 草稿
     */
    DRAFT(0, "草稿"),

    /**
     * 已发布
     */
    PUBLISHED(1, "已发布"),

    /**
     * 已停用
     */
    STOPPED(2, "已停用");

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
    public static ProcessStatusEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ProcessStatusEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
