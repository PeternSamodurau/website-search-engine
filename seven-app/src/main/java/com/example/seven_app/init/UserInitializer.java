package com.example.seven_app.init;

import com.example.seven_app.model.RoleType;
import com.example.seven_app.model.User;
import com.example.seven_app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("init")
@Order(1)
public class UserInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Принудительная переинициализация пользователей...");
        userRepository.deleteAll()
                .then(createTestUsers())
                .doOnSuccess(v -> log.info("Инициализация пользователей завершена успешно."))
                .doOnError(e -> log.error("Ошибка при инициализации пользователей: ", e))
                .subscribe();
    }

    private Mono<Void> createTestUsers() {
        User manager = new User();
        manager.setUsername("manager");
        manager.setEmail("manager@mail.ru");
        manager.setPassword(passwordEncoder.encode("manager"));
        manager.setRoles(Collections.singleton(RoleType.ROLE_MANAGER));

        return userRepository.save(manager)
                .thenMany(
                        Flux.range(1, 5)
                                .flatMap(i -> {
                                    User user = new User();
                                    user.setUsername("user" + i);
                                    user.setEmail("user" + i + "@mail.ru");
                                    user.setPassword(passwordEncoder.encode("user" + i));
                                    user.setRoles(Collections.singleton(RoleType.ROLE_USER));
                                    return userRepository.save(user);
                                })
                )
                .then();
    }
}
