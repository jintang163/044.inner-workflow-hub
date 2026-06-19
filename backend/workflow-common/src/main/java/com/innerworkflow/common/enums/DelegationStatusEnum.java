package com.innerworkflow.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DelegationStatusEnum {

    PENDING(0, "待生效"),
    ACTIVE(1, "生效中"),
    EXPIRED(2, "已过期"),
    REVOKED(3, "已撤销");

    private final Integer code;
    private final String desc;

    public static DelegationStatusEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (DelegationStatusEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
