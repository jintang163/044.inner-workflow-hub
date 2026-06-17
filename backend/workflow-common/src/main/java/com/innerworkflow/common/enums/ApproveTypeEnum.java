package com.innerworkflow.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 审批方式枚举
 *
 * @author InnerWorkflow Team
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum ApproveTypeEnum {

    /**
     * 或签：任一审批人同意即可通过
     */
    OR_SIGN(1, "或签"),

    /**
     * 会签：所有审批人同意才算通过
     */
    ALL_SIGN(2, "会签"),

    /**
     * 依次审批：按顺序逐个审批
     */
    SEQUENTIAL(3, "依次审批");

    /**
     * 方式码
     */
    private final Integer code;

    /**
     * 方式描述
     */
    private final String desc;

    /**
     * 根据方式码获取枚举
     */
    public static ApproveTypeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ApproveTypeEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
