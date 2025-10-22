package com.example.springbootnewsportal.aop.aspect;

import com.example.springbootnewsportal.model.RoleType;
import com.example.springbootnewsportal.model.User;
import com.example.springbootnewsportal.repository.UserRepository;
import com.example.springbootnewsportal.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class CheckPermissionAspect {

    private final UserRepository userRepository;

    @Before("@annotation(com.example.springbootnewsportal.aop.annotation.CheckPermission)")
    @Transactional(readOnly = true)
    public void checkPermission(JoinPoint joinPoint) {
        log.info("--- НАЧАЛО ПРОВЕРКИ ПРАВ ДОСТУПА (AOP) ---");

        // Шаг 1: Получаем аутентифицированного пользователя
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            log.error("ПРОВАЛ: Пользователь не аутентифицирован.");
            throw new AccessDeniedException("Доступ запрещен: требуется аутентификация.");
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
            throw new AccessDeniedException("У вас недостаточно прав для выполнения этого действия.");
        }
        log.info("Шаг 4: У пользователя есть необходимая роль. Продолжаем проверку.");

        // Получаем ID из аргументов метода, он понадобится в нескольких проверках
        Long requestedId = getRequestedId(joinPoint);

        // Шаг 5: Специальная проверка для ROLE_USER (может получить/изменить/удалить только себя)
        boolean isJustUser = currentUserRoles.contains(RoleType.ROLE_USER.name())
                && !currentUserRoles.contains(RoleType.ROLE_ADMIN.name())
                && !currentUserRoles.contains(RoleType.ROLE_MODERATOR.name());

        if (isJustUser) {
            log.info("Шаг 5: Пользователь имеет только роль ROLE_USER. Требуется проверка ID.");
            validateOwnerId(requestedId, securityUser.getId());
        }

        // Шаг 6: Специальная проверка для ROLE_MODERATOR (не может трогать админов)
        boolean isModeratorAndNotAdmin = currentUserRoles.contains(RoleType.ROLE_MODERATOR.name())
                && !currentUserRoles.contains(RoleType.ROLE_ADMIN.name());

        if (isModeratorAndNotAdmin) {
            log.info("Шаг 6: Пользователь является MODERATOR. Требуется проверка, не является ли цель администратором.");
            if (requestedId == null) {
                log.info("Шаг 6.1: ID цели не найден в аргументах, дополнительная проверка не требуется.");
            } else {
                log.info("Шаг 6.1: Проверяем цель с ID: {}", requestedId);
                User targetUser = userRepository.findById(requestedId)
                        .orElseThrow(() -> new AccessDeniedException("Целевой пользователь не найден."));

                log.info("Шаг 6.2: Роли целевого пользователя (ID: {}): {}", requestedId, targetUser.getRoles());

                // --- ИСПРАВЛЕННАЯ ЛОГИКА ПРОВЕРКИ ---
                boolean isTargetAdmin = targetUser.getRoles().contains(RoleType.ROLE_ADMIN);
                // ------------------------------------

                log.info("Шаг 6.3: Является ли цель администратором? -> {}", isTargetAdmin);

                if (isTargetAdmin) {
                    log.error("ПРОВАЛ: Модератор (ID: {}) пытается получить доступ к администратору (ID: {}).", securityUser.getId(), requestedId);
                    throw new AccessDeniedException("У вас недостаточно прав для выполнения этого действия. Модераторы не могут изменять администраторов.");
                }
                log.info("Шаг 6.4: Проверка пройдена. Цель не является администратором.");
            }
        }

        log.info("--- РЕЗУЛЬТАТ: УСПЕХ. Доступ разрешен. ---");
    }

    private Long getRequestedId(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (arg instanceof Long) {
                return (Long) arg;
            }
        }
        return null;
    }

    private void validateOwnerId(Long requestedId, Long securityUserId) {
        if (requestedId == null) {
            log.error("ПРОВАЛ: Для пользователя с ролью ROLE_USER не удалось найти ID в параметрах метода для проверки.");
            throw new AccessDeniedException("Невозможно проверить права доступа: ID не найден.");
        }
        log.info("Шаг 5.1: ID из запроса: {}. ID текущего пользователя: {}", requestedId, securityUserId);
        if (!Objects.equals(requestedId, securityUserId)) {
            log.error("ПРОВАЛ: Пользователь с ролью ROLE_USER (ID: {}) пытается получить доступ к чужому ресурсу (ID: {}).", securityUserId, requestedId);
            throw new AccessDeniedException("Вы можете изменять или просматривать только свои собственные данные.");
        }
        log.info("Шаг 5.2: Проверка ID пройдена. ID совпадают.");
    }
}
