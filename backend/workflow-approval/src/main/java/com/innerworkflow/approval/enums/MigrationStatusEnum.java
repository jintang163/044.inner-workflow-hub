package com.innerworkflow.approval.enums;

import lombok.Getter;

@Getter
public enum MigrationStatusEnum {

    PENDING(0, "待执行"),
    EXECUTING(1, "执行中"),
    SUCCESS(2, "全部成功"),
    PARTIAL_SUCCESS(3, "部分成功"),
    FAILED(4, "全部失败");

    private final Integer code;
    private final String name;

    MigrationStatusEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public static MigrationStatusEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (MigrationStatusEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }

    public static String getNameByCode(Integer code) {
        MigrationStatusEnum e = getByCode(code);
        return e != null ? e.getName() : "";
    }
}
