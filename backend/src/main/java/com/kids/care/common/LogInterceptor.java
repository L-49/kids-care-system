package com.kids.care.common;

import com.kids.care.entity.SysLog;
import com.kids.care.mapper.SysLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 操作日志拦截器：记录所有写操作（POST/PUT/DELETE）到 sys_log 表
 */
@Component
@RequiredArgsConstructor
public class LogInterceptor implements HandlerInterceptor {

    private static final Map<String, String> MODULE_NAME = Map.ofEntries(
            Map.entry("/api/child", "幼儿档案"),
            Map.entry("/api/clazz", "班级管理"),
            Map.entry("/api/attendance", "考勤管理"),
            Map.entry("/api/teacher", "教职工管理"),
            Map.entry("/api/course", "课程活动"),
            Map.entry("/api/fee", "收费管理"),
            Map.entry("/api/leave", "请假审批"),
            Map.entry("/api/notice", "通知公告"),
            Map.entry("/api/menu", "每周食谱"),
            Map.entry("/api/food", "食材库存"),
            Map.entry("/api/dict", "数据字典"),
            Map.entry("/api/auth", "登录认证"),
            Map.entry("/api/system", "系统设置")
    );

    private final SysLogMapper sysLogMapper;
    private final TokenStore tokenStore;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute("logStart", System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        String method = request.getMethod();
        if ("GET".equalsIgnoreCase(method) || "OPTIONS".equalsIgnoreCase(method)) {
            return;
        }
        try {
            Object start = request.getAttribute("logStart");
            long cost = start == null ? 0 : System.currentTimeMillis() - (Long) start;

            SysLog log = new SysLog();
            log.setUsername(resolveUsername(request));
            log.setOperation(buildOperation(method, request.getRequestURI()));
            log.setMethod(method);
            log.setUri(request.getRequestURI());
            String query = request.getQueryString();
            log.setParams(query == null ? "" : query.length() > 480 ? query.substring(0, 480) : query);
            log.setIp(request.getRemoteAddr());
            log.setCostTime(cost);
            log.setStatus(response.getStatus());
            // 失败原因：优先取全局异常处理器写入的 logError，其次取拦截器异常信息
            Object error = request.getAttribute("logError");
            if (error == null && ex != null) {
                error = ex.getMessage();
            }
            String errorMsg = error == null ? "" : String.valueOf(error);
            log.setErrorMsg(errorMsg.length() > 280 ? errorMsg.substring(0, 280) : errorMsg);
            log.setCreateTime(LocalDateTime.now());
            sysLogMapper.insert(log);
        } catch (Exception ignored) {
            // 日志记录失败不影响主流程
        }
    }

    private String resolveUsername(HttpServletRequest request) {
        // 登录接口还没有令牌，由 AuthController 写入尝试登录的账号
        Object attempted = request.getAttribute("logUsername");
        if (attempted != null) {
            return String.valueOf(attempted);
        }
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        TokenStore.LoginUser user = tokenStore.get(token);
        return user == null ? "匿名" : user.realName();
    }

    private String buildOperation(String method, String uri) {
        String module = "系统";
        for (Map.Entry<String, String> e : MODULE_NAME.entrySet()) {
            if (uri.startsWith(e.getKey())) {
                module = e.getValue();
                break;
            }
        }
        if (uri.contains("/login")) {
            return "登录系统";
        }
        if (uri.contains("/logout")) {
            return "退出登录";
        }
        if (uri.contains("/pay")) {
            return "缴费登记（" + module + "）";
        }
        if (uri.contains("/approve")) {
            return "审批处理（" + module + "）";
        }
        if (uri.contains("/transfer")) {
            return "调班（" + module + "）";
        }
        if (uri.contains("/backup")) {
            return "数据备份";
        }
        if (uri.endsWith("/clean")) {
            return "清空日志";
        }
        String action = switch (method.toUpperCase()) {
            case "POST" -> "新增";
            case "PUT" -> "修改";
            case "DELETE" -> "删除";
            default -> "操作";
        };
        return action + module;
    }
}
