package com.innerworkflow.notify.enums;

import lombok.Getter;

@Getter
public enum EventTypeEnum {

    TASK_CREATE("TASK_CREATE", "任务创建"),
    TASK_COMPLETE("TASK_COMPLETE", "任务完成"),
    PROCESS_START("PROCESS_START", "流程发起"),
    PROCESS_END("PROCESS_END", "流程结束"),
    TIMEOUT_REMIND("TIMEOUT_REMIND", "超时提醒"),
    CC_NOTIFY("CC_NOTIFY", "抄送通知"),
    CC_REMIND("CC_REMIND", "抄送催读");

    private final String code;
    private final String name;

    EventTypeEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static EventTypeEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (EventTypeEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return null;
    }
}
