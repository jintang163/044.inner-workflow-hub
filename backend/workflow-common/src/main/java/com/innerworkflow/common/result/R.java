package com.innerworkflow.common.result;

import com.innerworkflow.common.enums.ResultCode;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 统一响应结果类
 * <p>
 * 用于封装所有REST接口的返回数据，保持接口响应格式一致
 * </p>
 *
 * @param <T> 返回数据的类型
 * @author InnerWorkflow Team
 * @since 1.0.0
 */
@Data
public class R<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 响应状态码
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 成功响应（无数据）
     */
    public static <T> R<T> success() {
        return build(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    /**
     * 成功响应（带数据）
     */
    public static <T> R<T> success(T data) {
        return build(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    /**
     * 成功响应（自定义消息）
     */
    public static <T> R<T> success(String message, T data) {
        return build(ResultCode.SUCCESS.getCode(), message, data);
    }

    /**
     * 失败响应（使用默认错误码）
     */
    public static <T> R<T> fail() {
        return build(ResultCode.INTERNAL_SERVER_ERROR.getCode(), ResultCode.INTERNAL_SERVER_ERROR.getMessage(), null);
    }

    /**
     * 失败响应（自定义消息）
     */
    public static <T> R<T> fail(String message) {
        return build(ResultCode.INTERNAL_SERVER_ERROR.getCode(), message, null);
    }

    /**
     * 失败响应（自定义错误码和消息）
     */
    public static <T> R<T> fail(Integer code, String message) {
        return build(code, message, null);
    }

    /**
     * 失败响应（使用ResultCode枚举）
     */
    public static <T> R<T> fail(ResultCode resultCode) {
        return build(resultCode.getCode(), resultCode.getMessage(), null);
    }

    /**
     * 失败响应（使用ResultCode枚举，自定义消息）
     */
    public static <T> R<T> fail(ResultCode resultCode, String message) {
        return build(resultCode.getCode(), message, null);
    }

    /**
     * 错误响应（默认500错误）
     */
    public static <T> R<T> error() {
        return fail();
    }

    /**
     * 错误响应（自定义消息）
     */
    public static <T> R<T> error(String message) {
        return fail(message);
    }

    /**
     * 判断响应是否成功
     */
    public boolean isSuccess() {
        return ResultCode.SUCCESS.getCode().equals(this.code);
    }

    /**
     * 构建响应对象
     */
    private static <T> R<T> build(Integer code, String message, T data) {
        R<T> result = new R<>();
        result.setCode(code);
        result.setMessage(message);
        result.setData(data);
        return result;
    }
}
