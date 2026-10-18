package com.kids.care.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 登录校验 + 角色鉴权拦截器。
 * <p>先校验令牌，再读取 {@link RequireRole}（方法优先、类其次）判断当前角色是否允许访问。</p>
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final TokenStore tokenStore;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        // 线程复用场景下先清理，避免上一次请求的登录人残留
        UserContext.clear();
        String token = resolveToken(request);
        TokenStore.LoginUser user = tokenStore.get(token);
        if (user == null) {
            writeJson(response, 401, "登录已过期，请重新登录");
            return false;
        }
        // 写入上下文，供业务层做数据范围过滤（家长只能看自己孩子）
        UserContext.set(user);
        request.setAttribute("loginUser", user);

        if (handler instanceof HandlerMethod method) {
            RequireRole required = method.getMethodAnnotation(RequireRole.class);
            if (required == null) {
                required = method.getBeanType().getAnnotation(RequireRole.class);
            }
            if (required != null && !Arrays.asList(required.value()).contains(user.role())) {
                String allowed = Arrays.stream(required.value())
                        .map(Roles::label).collect(Collectors.joining("、"));
                // 拒绝访问时 afterCompletion 不会执行，这里必须主动清理
                UserContext.clear();
                writeJson(response, 403, "当前账号（" + Roles.label(user.role()) + "）无权访问该功能，仅限：" + allowed);
                return false;
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private String resolveToken(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            return token.substring(7);
        }
        return token;
    }

    private void writeJson(HttpServletResponse response, int status, String msg) throws Exception {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":" + status + ",\"msg\":\"" + msg + "\"}");
    }
}
