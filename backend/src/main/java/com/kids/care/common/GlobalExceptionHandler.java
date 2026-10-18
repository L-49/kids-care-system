package com.kids.care.common;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理：对外只返回可读提示，异常堆栈只写服务端日志。
 * <p>HTTP 状态码与业务码保持一致，便于操作日志记录真实结果。</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<Result<Void>> handleBiz(BizException e, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, e.getMessage(), request);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<Result<Void>> handleValid(Exception e, HttpServletRequest request) {
        String msg = "参数校验失败";
        if (e instanceof MethodArgumentNotValidException ex && ex.getBindingResult().getFieldError() != null) {
            msg = ex.getBindingResult().getFieldError().getDefaultMessage();
        } else if (e instanceof BindException ex && ex.getBindingResult().getFieldError() != null) {
            msg = ex.getBindingResult().getFieldError().getDefaultMessage();
        }
        return build(HttpStatus.BAD_REQUEST, msg, request);
    }

    /** 唯一键冲突：软删除后重录、重复录入同一日期同一餐次等 */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Result<Void>> handleDuplicate(DuplicateKeyException e, HttpServletRequest request) {
        String raw = String.valueOf(e.getMostSpecificCause().getMessage());
        String msg = "数据重复，已存在相同记录";
        if (raw.contains("uk_menu")) {
            msg = "该日期同一餐次的食谱已存在，请直接编辑原记录";
        } else if (raw.contains("uk_att")) {
            msg = "该幼儿当天已有考勤记录，请直接编辑原记录";
        } else if (raw.contains("username")) {
            msg = "登录名已存在，请更换";
        } else if (raw.contains("name")) {
            msg = "名称已存在，请更换";
        }
        log.warn("唯一键冲突 uri={} detail={}", request.getRequestURI(), raw);
        return build(HttpStatus.CONFLICT, msg, request);
    }

    /** 外键/非空/长度等数据完整性问题 */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Result<Void>> handleDataIntegrity(DataIntegrityViolationException e,
                                                           HttpServletRequest request) {
        log.warn("数据完整性异常 uri={} detail={}", request.getRequestURI(),
                e.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT, "数据不合法或存在关联，无法保存", request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleUnreadable(HttpMessageNotReadableException e,
                                                         HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "请求参数格式不正确", request);
    }

    /** 访问了不存在的接口 */
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<Result<Void>> handleNotFound(Exception e, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "接口不存在，请检查请求地址", request);
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Result<Void>> handleParam(Exception e, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "请求参数缺失或类型不正确", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleOther(Exception e, HttpServletRequest request) {
        // 完整堆栈只写日志，绝不返回给前端
        log.error("服务器内部错误 uri={}", request.getRequestURI(), e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "服务器处理失败，请稍后重试或联系管理员", request);
    }

    private ResponseEntity<Result<Void>> build(HttpStatus status, String msg, HttpServletRequest request) {
        // 供 LogInterceptor 记录失败原因
        request.setAttribute("logError", msg);
        return ResponseEntity.status(status).body(Result.fail(status.value(), msg));
    }
}
