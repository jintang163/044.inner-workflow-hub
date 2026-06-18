package com.innerworkflow.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * BPMN节点类型枚举
 *
 * @author InnerWorkflow Team
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum NodeTypeEnum {

    /**
     * 开始事件
     */
    START_EVENT("START_EVENT", "开始节点"),

    /**
     * 用户任务
     */
    USER_TASK("USER_TASK", "审批节点"),

    /**
     * 排他网关
     */
    EXCLUSIVE_GATEWAY("EXCLUSIVE_GATEWAY", "条件分支"),

    /**
     * 并行网关
     */
    PARALLEL_GATEWAY("PARALLEL_GATEWAY", "并行分支"),

    /**
     * 调用活动（子流程）
     */
    CALL_ACTIVITY("CALL_ACTIVITY", "子流程"),

    /**
     * 结束事件
     */
    END_EVENT("END_EVENT", "结束节点");

    /**
     * 节点类型编码
     */
    private final String code;

    /**
     * 节点类型描述
     */
    private final String desc;

    /**
     * 根据类型编码获取枚举
     */
    public static NodeTypeEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (NodeTypeEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
