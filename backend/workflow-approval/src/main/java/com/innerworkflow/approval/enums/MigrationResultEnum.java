package com.innerworkflow.approval.enums;

import lombok.Getter;

@Getter
public enum MigrationResultEnum {

    PENDING(0, "待迁移"),
    SUCCESS(1, "迁移成功"),
    FAILED(2, "迁移失败"),
    SKIPPED(3, "跳过");

    private final Integer code;
    private final String name;

    MigrationResultEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public static MigrationResultEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (MigrationResultEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }

    public static String getNameByCode(Integer code) {
        MigrationResultEnum e = getByCode(code);
        return e != null ? e.getName() : "";
    }
}
