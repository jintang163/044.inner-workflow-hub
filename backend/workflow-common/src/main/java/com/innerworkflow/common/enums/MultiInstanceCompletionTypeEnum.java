package com.innerworkflow.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MultiInstanceCompletionTypeEnum {

    ALL_PASS(1, "全部通过"),

    ANY_PASS(2, "任一通过"),

    PERCENTAGE_PASS(3, "百分比通过");

    private final Integer code;

    private final String desc;

    public static MultiInstanceCompletionTypeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (MultiInstanceCompletionTypeEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
