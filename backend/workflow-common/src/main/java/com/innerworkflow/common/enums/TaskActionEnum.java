package com.innerworkflow.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 任务操作类型枚举
 *
 * @author InnerWorkflow Team
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum TaskActionEnum {

    /**
     * 同意
     */
    AGREE(1, "同意"),

    /**
     * 拒绝
     */
    REJECT(2, "拒绝"),

    /**
     * 转审
     */
    TRANSFER(3, "转审"),

    /**
     * 加签
     */
    ADD_SIGN(4, "加签"),

    /**
     * 委派
     */
    DELEGATE(5, "委派"),

    /**
     * 驳回
     */
    SEND_BACK(6, "驳回"),

    /**
     * 撤回
     */
    WITHDRAW(7, "撤回"),

    /**
     * 超时自动处理
     */
    TIMEOUT_AUTO(8, "超时自动");

    /**
     * 操作码
     */
    private final Integer code;

    /**
     * 操作描述
     */
    private final String desc;

    /**
     * 根据操作码获取枚举
     */
    public static TaskActionEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (TaskActionEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
