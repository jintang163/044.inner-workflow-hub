package com.innerworkflow.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统一响应结果码枚举
 *
 * @author InnerWorkflow Team
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    /**
     * 成功
     */
    SUCCESS(200, "操作成功"),

    /**
     * 未认证
     */
    UNAUTHORIZED(401, "未认证或Token已过期"),

    /**
     * 无权限
     */
    FORBIDDEN(403, "无权限访问"),

    /**
     * 资源不存在
     */
    NOT_FOUND(404, "请求的资源不存在"),

    /**
     * 服务器内部错误
     */
    INTERNAL_SERVER_ERROR(500, "服务器内部错误"),

    /**
     * 参数错误
     */
    PARAM_ERROR(1001, "参数校验失败"),

    /**
     * 流程错误
     */
    PROCESS_ERROR(2001, "流程处理异常");

    /**
     * 状态码
     */
    private final Integer code;

    /**
     * 提示信息
     */
    private final String message;
}
