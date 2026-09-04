package com.example.test.security;

/**
 * 当前登录用户上下文：认证过滤器写入，业务代码随时读取，对应 Express 侧 req.user。
 * <p>使用自管理 ThreadLocal（认证过滤器负责 set/clear），不依赖 RequestContextHolder 的绑定时机。</p>
 */
public final class AuthContext {

    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    private AuthContext() {
    }

    public static void set(CurrentUser user) {
        HOLDER.set(user);
    }

    public static void clear() {
        HOLDER.remove();
    }

    public static CurrentUser get() {
        return HOLDER.get();
    }

    /**
     * 必须已登录，否则抛出 401
     */
    public static CurrentUser require() {
        CurrentUser user = get();
        if (user == null) {
            throw new com.example.test.common.AppException(401, "请先登录", "UNAUTHORIZED");
        }
        return user;
    }
}
