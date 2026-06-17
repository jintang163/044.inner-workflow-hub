package com.innerworkflow.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 审批历史活动类型枚举
 *
 * @author InnerWorkflow Team
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum HistoryActivityTypeEnum {

    /**
     * 发起流程
     */
    START(1, "发起"),

    /**
     * 通过审批
     */
    APPROVE(2, "通过"),

    /**
     * 拒绝审批
     */
    REJECT(3, "拒绝"),

    /**
     * 转审
     */
    TRANSFER(4, "转审"),

    /**
     * 加签
     */
    ADD_SIGN(5, "加签"),

    /**
     * 委派
     */
    DELEGATE(6, "委派"),

    /**
     * 驳回
     */
    SEND_BACK(7, "驳回"),

    /**
     * 撤回
     */
    WITHDRAW(8, "撤回"),

    /**
     * 抄送
     */
    CC(9, "抄送"),

    /**
     * 超时自动处理
     */
    TIMEOUT_AUTO(10, "超时自动"),

    /**
     * 流程结束
     */
    PROCESS_END(11, "流程结束");

    /**
     * 活动类型码
     */
    private final Integer code;

    /**
     * 活动类型描述
     */
    private final String desc;

    /**
     * 根据类型码获取枚举
     */
    public static HistoryActivityTypeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (HistoryActivityTypeEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
