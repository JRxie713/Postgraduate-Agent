package com.demo.scaffoldbackend.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    SUCCESS(200, "success"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或 Token 失效"),
    FORBIDDEN(403, "没有权限访问"),
    NOT_FOUND(404, "资源不存在"),
    SERVER_ERROR(500, "服务器内部错误");

    private final Integer code;
    private final String message;
}
