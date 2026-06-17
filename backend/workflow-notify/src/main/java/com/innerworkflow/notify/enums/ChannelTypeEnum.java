package com.innerworkflow.notify.enums;

import lombok.Getter;

@Getter
public enum ChannelTypeEnum {

    DINGTALK("DINGTALK", "钉钉"),
    WECOM("WECOM", "企业微信"),
    EMAIL("EMAIL", "邮件"),
    SMS("SMS", "短信");

    private final String code;
    private final String name;

    ChannelTypeEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static ChannelTypeEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (ChannelTypeEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return null;
    }
}
