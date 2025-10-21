package com.example.springbootnewsportal.aop.aspect;

import com.example.springbootnewsportal.model.RoleType;
import com.example.springbootnewsportal.security.SecurityUser;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.access.AccessDeniedException; // ИЗМЕНЕНО
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Aspect
@Component
@Slf4j
public class CheckPermissionAspect {

    @Before("@annotation(com.example.springbootnewsportal.aop.annotation.CheckPermission)")
    public void checkPermission(JoinPoint joinPoint) {
        log.info("--- НАЧАЛО ПРОВЕРКИ ПРАВ ДОСТУПА (AOP) ---");

        // Шаг 1: Получаем аутентифицированного пользователя
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            log.error("ПРОВАЛ: Пользователь не аутентифицирован.");
            throw new AccessDeniedException("Доступ запрещен: требуется аутентификация."); // ИЗМЕНЕНО
        }
        SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();
        log.info("Шаг 1: Пользователь аутентифицирован как '{}' (ID: {})",
                securityUser.getUsername(), securityUser.getId());

        // Шаг 2: Получаем роли текущего пользователя
        Set<String> currentUserRoles = securityUser.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        log.info("Шаг 2: Роли пользователя: {}", currentUserRoles);

        // Шаг 3: Получаем требуемые роли из аннотации
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        com.example.springbootnewsportal.aop.annotation.CheckPermission checkPermissionAnnotation = method.getAnnotation(com.example.springbootnewsportal.aop.annotation.CheckPermission.class);
        Set<String> requiredRoles = Set.of(checkPermissionAnnotation.value());
        log.info("Шаг 3: Метод '{}' требует одну из ролей: {}", method.getName(), requiredRoles);

        // Шаг 4: Проверяем, есть ли у пользователя хотя бы одна из требуемых ролей
        boolean hasRequiredRole = requiredRoles.stream().anyMatch(currentUserRoles::contains);
        if (!hasRequiredRole) {
            log.error("ПРОВАЛ: У пользователя нет ни одной из требуемых ролей. Доступ запрещен.");
            throw new AccessDeniedException("У вас недостаточно прав для выполнения этого действия."); // ИЗМЕНЕНО
        }
        log.info("Шаг 4: У пользователя есть необходимая роль. Продолжаем проверку.");

        // Шаг 5: Специальная проверка для ROLE_USER (может получить/изменить/удалить только себя)
        // Эта проверка нужна, только если у пользователя НЕТ прав админа или модератора
        boolean isJustUser = currentUserRoles.contains(RoleType.ROLE_USER.name())
                && !currentUserRoles.contains(RoleType.ROLE_ADMIN.name())
                && !currentUserRoles.contains(RoleType.ROLE_MODERATOR.name());

        if (isJustUser) {
            log.info("Шаг 5: Пользователь имеет только роль ROLE_USER. Требуется проверка ID.");
            // Ищем ID в аргументах метода
            Object[] args = joinPoint.getArgs();
            Long requestedId = null;
            for (Object arg : args) {
                if (arg instanceof Long) {
                    requestedId = (Long) arg;
                    break;
                }
            }

            if (requestedId == null) {
                // Если метод должен был иметь ID, но его нет - это ошибка конфигурации или вызова
                log.error("ПРОВАЛ: Для пользователя с ролью ROLE_USER не удалось найти ID в параметрах метода для проверки.");
                throw new AccessDeniedException("Невозможно проверить права доступа: ID не найден."); // ИЗМЕНЕНО
            }

            log.info("Шаг 5.1: ID из запроса: {}. ID текущего пользователя: {}", requestedId, securityUser.getId());
            if (!Objects.equals(requestedId, securityUser.getId())) {
                log.error("ПРОВАЛ: Пользователь с ролью ROLE_USER (ID: {}) пытается получить доступ к чужому ресурсу (ID: {}).", securityUser.getId(), requestedId);
                throw new AccessDeniedException("Вы можете изменять или просматривать только свои собственные данные."); // ИЗМЕНЕНО
            }
            log.info("Шаг 5.2: Проверка ID пройдена. ID совпадают.");
        } else {
            log.info("Шаг 5: Пользователь является ADMIN или MODERATOR. Проверка ID не требуется.");
        }

        log.info("--- РЕЗУЛЬТАТ: УСПЕХ. Доступ разрешен. ---");
    }
}