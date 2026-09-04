package com.example.test.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口权限注解，对应 Express 侧 authorize('system:user:list') 中间件
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {

    /**
     * 满足任一权限即可通过；admin 角色直接放行
     */
    String[] value() default {};
}
