package com.demo.scaffoldbackend.common.exception;

/**
 * 业务异常：携带业务码，由全局异常处理器统一转为 Result
 */
public class BusinessException extends RuntimeException {

    /** 业务状态码（401/403/404/409/423/429 等） */
    private final Integer code;

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }
}
