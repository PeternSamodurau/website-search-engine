package com.example.springbootnewsportal.aop.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Аннотация для проверки прав доступа к методу.
 * Позволяет указать, какой ролью должен обладать пользователь,
 * чтобы выполнить данный метод.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CheckPermission {

    /**
     * Роль, необходимая для доступа.
     * Например, "ROLE_ADMIN".
     */
    String value();
}