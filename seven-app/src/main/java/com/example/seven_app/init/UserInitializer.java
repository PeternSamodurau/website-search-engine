package com.example.seven_app.init;

import com.example.seven_app.model.RoleType;
import com.example.seven_app.model.User;
import com.example.seven_app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;

@Component
@Profile("init")
@RequiredArgsConstructor
public class UserInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        userRepository.count()
                .flatMap(count -> {
                    if (count == 0) {
                        return createTestUsers();
                    }
                    return Mono.empty();
                })
                .subscribe();
    }

    private Mono<Void> createTestUsers() {
        return Flux.range(1, 5)
                .flatMap(i -> {
                    User user = new User();
                    user.setUsername("user" + i + "@example.com");
                    user.setEmail("user" + i + "@example.com");

                    // Устанавливаем пароли: password1, password2, password3 и т.д.
                    user.setPassword(passwordEncoder.encode("password" + i));

                    if (i == 1) {
                        user.setRoles(Set.of(RoleType.ROLE_USER, RoleType.ROLE_MANAGER));
                    } else {
                        user.setRoles(Set.of(RoleType.ROLE_USER));
                    }
                    return userRepository.save(user);
                })
                .then();
    }
}
