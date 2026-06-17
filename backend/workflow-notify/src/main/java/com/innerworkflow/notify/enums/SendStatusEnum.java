package com.innerworkflow.notify.enums;

import lombok.Getter;

@Getter
public enum SendStatusEnum {

    PENDING(0, "待发送"),
    SENDING(1, "发送中"),
    SUCCESS(2, "发送成功"),
    FAILED(3, "发送失败");

    private final Integer code;
    private final String name;

    SendStatusEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public static SendStatusEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (SendStatusEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return null;
    }
}
