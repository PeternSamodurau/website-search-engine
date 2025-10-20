package com.example.springbootnewsportal.aop.aspect;

import com.example.springbootnewsportal.aop.annotation.CheckPermission;
import com.example.springbootnewsportal.exception.AccessDeniedException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.stream.Collectors;

@Aspect
@Component
@Slf4j
public class CheckPermissionAspect {

    @Around("@annotation(com.example.springbootnewsportal.aop.annotation.CheckPermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint) throws Throwable {
        log.info("--- НАЧАЛО ПРОВЕРКИ ПРАВ ДОСТУПА ---");

        // 1. Получаем информацию о текущем пользователе
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            log.error("ПРОВЕРКА ПРОВАЛЕНА: Пользователь не аутентифицирован или является анонимным.");
            throw new AccessDeniedException("Пользователь не аутентифицирован.");
        }
        log.info("Шаг 1: Пользователь аутентифицирован как '{}'.", authentication.getName());


        // 2. Получаем требуемую роль из аннотации
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        CheckPermission checkPermissionAnnotation = method.getAnnotation(CheckPermission.class);
        String requiredRole = checkPermissionAnnotation.value();
        log.info("Шаг 2: Метод '{}' требует роль '{}'.", joinPoint.getSignature().toShortString(), requiredRole);


        // 3. Получаем роли текущего пользователя
        Set<String> userRoles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        log.info("Шаг 3: Роли пользователя '{}': {}", authentication.getName(), userRoles);


        // 4. Проверяем, есть ли у пользователя требуемая роль
        log.info("Шаг 4: Проверяем наличие роли '{}' в списке ролей пользователя.", requiredRole);

        if (userRoles.contains(requiredRole)) {
            log.info("РЕЗУЛЬТАТ: УСПЕХ. Доступ разрешен.");
            log.info("--- КОНЕЦ ПРОВЕРКИ ПРАВ ДОСТУПА ---");
            // Если роль есть, выполняем основной метод
            return joinPoint.proceed();
        } else {
            // Если роли нет, выбрасываем исключение
            log.warn("РЕЗУЛЬТАТ: ОТКАЗ. У пользователя '{}' отсутсвует требуемая роль '{}'.", authentication.getName(), requiredRole);
            log.info("--- КОНЕЦ ПРОВЕРКИ ПРАВ ДОСТУПА ---");
            throw new AccessDeniedException("Недостаточно прав для выполнения операции.");
        }
    }
}
