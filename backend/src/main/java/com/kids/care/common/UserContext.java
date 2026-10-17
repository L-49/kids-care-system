package com.kids.care.common;

/**
 * 当前登录用户上下文（ThreadLocal）。
 *
 * <p>由 {@code AuthInterceptor} 在校验令牌通过后写入、请求结束时清理。
 * 有了它，业务代码才能回答"这条档案是谁改的"——变更留痕、定向可见都依赖它。
 *
 * <p>注意：@Scheduled 定时任务所在的线程没有登录态，取到的会是空，
 * 调用方需要自己兜底（例如记成「系统」）。
 */
public final class UserContext {

    private static final ThreadLocal<TokenStore.LoginUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(TokenStore.LoginUser user) {
        HOLDER.set(user);
    }

    public static TokenStore.LoginUser get() {
        return HOLDER.get();
    }

    /** 操作人展示名：优先真实姓名，其次登录名，都没有则记为「系统」 */
    public static String displayName() {
        TokenStore.LoginUser u = HOLDER.get();
        if (u == null) {
            return "系统";
        }
        if (u.realName() != null && !u.realName().isBlank()) {
            return u.realName();
        }
        return u.username() == null ? "系统" : u.username();
    }

    public static String role() {
        TokenStore.LoginUser u = HOLDER.get();
        return u == null ? null : u.role();
    }

    public static Long userId() {
        TokenStore.LoginUser u = HOLDER.get();
        return u == null ? null : u.userId();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
