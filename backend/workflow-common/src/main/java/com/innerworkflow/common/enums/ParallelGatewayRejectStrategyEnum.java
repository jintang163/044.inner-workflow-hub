package com.innerworkflow.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ParallelGatewayRejectStrategyEnum {

    TERMINATE_OTHERS(1, "立即终止其他分支"),

    WAIT_ALL_COMPLETE(2, "等待全部完成后驳回");

    private final Integer code;
    private final String desc;

    public static ParallelGatewayRejectStrategyEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ParallelGatewayRejectStrategyEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
