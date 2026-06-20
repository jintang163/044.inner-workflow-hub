package com.innerworkflow.approval.enums;

import lombok.Getter;

@Getter
public enum CommentTemplateScopeEnum {

    PERSONAL(0, "个人模板"),
    DEPT_PUBLIC(1, "部门公共模板"),
    GLOBAL(2, "全局模板");

    private final Integer code;
    private final String name;

    CommentTemplateScopeEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public static CommentTemplateScopeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (CommentTemplateScopeEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return null;
    }
}
