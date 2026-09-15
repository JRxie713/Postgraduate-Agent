package com.demo.scaffoldbackend.common.exception;

import com.demo.scaffoldbackend.common.enums.ErrorCode;
import com.demo.scaffoldbackend.common.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusiness(BusinessException e) {
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 参数校验异常（@Valid 触发）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse(ErrorCode.BAD_REQUEST.getMessage());
        return Result.error(ErrorCode.BAD_REQUEST.getCode(), msg);
    }

    /**
     * 非法参数
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<?> handleIllegalArgument(IllegalArgumentException e) {
        return Result.error(ErrorCode.BAD_REQUEST.getCode(), e.getMessage());
    }

    /**
     * 权限不足
     */
    @ExceptionHandler(AccessDeniedException.class)
    public Result<?> handleAccessDenied(AccessDeniedException e) {
        return Result.error(ErrorCode.FORBIDDEN.getCode(), ErrorCode.FORBIDDEN.getMessage());
    }

    /**
     * 兜底异常
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("系统异常: ", e);
        return Result.error(ErrorCode.SERVER_ERROR.getCode(), "服务器内部异常，请稍后重试");
    }
}