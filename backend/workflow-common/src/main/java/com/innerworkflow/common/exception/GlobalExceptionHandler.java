package com.innerworkflow.common.exception;

import com.innerworkflow.common.enums.ResultCode;
import com.innerworkflow.common.result.R;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * <p>
 * 统一处理各类异常，返回标准格式的响应结果
 * </p>
 *
 * @author InnerWorkflow Team
 * @since 1.0.0
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理自定义业务异常
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public R<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }

    /**
     * 处理 @Valid @RequestBody 参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.OK)
    public R<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数校验异常(@RequestBody): {}", message);
        return R.fail(ResultCode.PARAM_ERROR, message);
    }

    /**
     * 处理 @Valid @ModelAttribute 参数绑定异常
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.OK)
    public R<Void> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数绑定异常(@ModelAttribute): {}", message);
        return R.fail(ResultCode.PARAM_ERROR, message);
    }

    /**
     * 处理 @Validated 方法级参数校验异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.OK)
    public R<Void> handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数校验异常(方法级): {}", message);
        return R.fail(ResultCode.PARAM_ERROR, message);
    }

    /**
     * 处理请求参数缺失异常
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.OK)
    public R<Void> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        String message = String.format("缺少必要的请求参数: %s", e.getParameterName());
        log.warn(message);
        return R.fail(ResultCode.PARAM_ERROR, message);
    }

    /**
     * 处理请求参数类型转换异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.OK)
    public R<Void> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        String message = String.format("参数 '%s' 类型转换失败", e.getName());
        log.warn(message);
        return R.fail(ResultCode.PARAM_ERROR, message);
    }

    /**
     * 处理请求体解析异常（如JSON格式错误）
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.OK)
    public R<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("请求体解析异常: {}", e.getMessage());
        return R.fail(ResultCode.PARAM_ERROR, "请求体格式错误");
    }

    /**
     * 处理请求方法不支持异常
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.OK)
    public R<Void> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        String message = String.format("不支持的请求方法: %s", e.getMethod());
        log.warn(message);
        return R.fail(ResultCode.PARAM_ERROR, message);
    }

    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.OK)
    public R<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("非法参数异常: {}", e.getMessage());
        return R.fail(ResultCode.PARAM_ERROR, e.getMessage());
    }

    /**
     * 处理其他所有未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<Void> handleException(Exception e) {
        log.error("系统异常: ", e);
        return R.fail(ResultCode.INTERNAL_SERVER_ERROR);
    }
}
