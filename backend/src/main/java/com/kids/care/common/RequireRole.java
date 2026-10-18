package com.kids.care.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口角色白名单。
 * <p>标在 Controller 类上作为默认规则，标在方法上覆盖类规则；
 * 未标注的接口默认对所有已登录角色开放。</p>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {

    /** 允许访问的角色，取值见 {@link Roles} */
    String[] value();
}
