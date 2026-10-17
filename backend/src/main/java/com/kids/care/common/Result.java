package com.kids.care.common;

import lombok.Data;

/**
 * 统一响应结果
 */
@Data
public class Result<T> {
    private int code;
    private String msg;
    private T data;
    private Long total;

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.code = 200;
        r.msg = "操作成功";
        r.data = data;
        return r;
    }

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> page(T data, long total) {
        Result<T> r = ok(data);
        r.total = total;
        return r;
    }

    public static <T> Result<T> fail(String msg) {
        Result<T> r = new Result<>();
        r.code = 500;
        r.msg = msg;
        return r;
    }
}
