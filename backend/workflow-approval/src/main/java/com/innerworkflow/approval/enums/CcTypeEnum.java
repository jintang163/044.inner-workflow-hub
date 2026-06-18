package com.innerworkflow.approval.enums;

import lombok.Getter;

@Getter
public enum CcTypeEnum {

    MANUAL(1, "手动抄送"),
    AUTO_NODE_START(2, "节点启动自动抄送"),
    AUTO_NODE_COMPLETE(3, "节点完成自动抄送"),
    AUTO_PROCESS_END(4, "流程结束自动抄送");

    private final Integer code;
    private final String name;

    CcTypeEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public static CcTypeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (CcTypeEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return null;
    }
}
