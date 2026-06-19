package com.innerworkflow.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TransferTypeEnum {

    MANUAL(1, "手动转审"),
    DELEGATION(2, "委托自动转审"),
    BATCH(3, "批量转审");

    private final Integer code;
    private final String desc;

    public static TransferTypeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (TransferTypeEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
