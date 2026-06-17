package com.innerworkflow.common.exception;

import com.innerworkflow.common.enums.ResultCode;
import lombok.Getter;

import java.io.Serial;

/**
 * 业务异常类
 * <p>
 * 用于处理业务逻辑中的异常情况，携带错误码和错误消息
 * </p>
 *
 * @author InnerWorkflow Team
 * @since 1.0.0
 */
@Getter
public class BusinessException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    private final Integer code;

    /**
     * 错误消息
     */
    private final String message;

    /**
     * 使用ResultCode枚举创建异常
     */
    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
        this.message = resultCode.getMessage();
    }

    /**
     * 使用ResultCode枚举和自定义消息创建异常
     */
    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
        this.message = message;
    }

    /**
     * 使用自定义错误码和消息创建异常
     */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    /**
     * 使用自定义消息创建异常（默认500错误码）
     */
    public BusinessException(String message) {
        super(message);
        this.code = ResultCode.INTERNAL_SERVER_ERROR.getCode();
        this.message = message;
    }

    /**
     * 便捷方法：抛出参数错误异常
     */
    public static BusinessException paramError(String message) {
        return new BusinessException(ResultCode.PARAM_ERROR, message);
    }

    /**
     * 便捷方法：抛出流程错误异常
     */
    public static BusinessException processError(String message) {
        return new BusinessException(ResultCode.PROCESS_ERROR, message);
    }

    /**
     * 便捷方法：抛出无权限异常
     */
    public static BusinessException forbidden(String message) {
        return new BusinessException(ResultCode.FORBIDDEN, message);
    }

    /**
     * 便捷方法：抛出资源不存在异常
     */
    public static BusinessException notFound(String message) {
        return new BusinessException(ResultCode.NOT_FOUND, message);
    }
}
