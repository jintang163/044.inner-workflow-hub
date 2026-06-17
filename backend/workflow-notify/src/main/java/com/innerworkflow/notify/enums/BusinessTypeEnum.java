package com.innerworkflow.notify.enums;

import lombok.Getter;

@Getter
public enum BusinessTypeEnum {

    APPROVAL("APPROVAL", "审批通知"),
    TIMEOUT("TIMEOUT", "超时通知"),
    SYSTEM("SYSTEM", "系统通知");

    private final String code;
    private final String name;

    BusinessTypeEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static BusinessTypeEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (BusinessTypeEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return null;
    }
}
