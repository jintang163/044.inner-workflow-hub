package com.innerworkflow.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MultiInstanceSignStatusEnum {

    PENDING(0, "待处理"),

    APPROVED(1, "已通过"),

    REJECTED(2, "已拒绝");

    private final Integer code;

    private final String desc;

    public static MultiInstanceSignStatusEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (MultiInstanceSignStatusEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
