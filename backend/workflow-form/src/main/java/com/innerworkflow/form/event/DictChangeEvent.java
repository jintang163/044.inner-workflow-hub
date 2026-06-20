package com.innerworkflow.form.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class DictChangeEvent extends ApplicationEvent {

    private final String dictCode;
    private final String changeType;

    public DictChangeEvent(Object source, String dictCode, String changeType) {
        super(source);
        this.dictCode = dictCode;
        this.changeType = changeType;
    }
}
