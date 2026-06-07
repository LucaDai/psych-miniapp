package com.psych.miniapp.common.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    SUCCESS(0, "ok"),
    BAD_REQUEST(40001, "请求参数错误"),
    UNAUTHORIZED(40101, "未登录或 Token 无效"),
    FORBIDDEN(40301, "无权限"),
    NOT_FOUND(40401, "资源不存在"),
    CONFLICT(40901, "业务冲突"),
    UNPROCESSABLE(42201, "业务校验失败"),
    INTERNAL_ERROR(50001, "服务器内部错误");

    private final int code;
    private final String message;
}
