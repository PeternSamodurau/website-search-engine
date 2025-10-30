package com.example.seven_app.init;

import com.example.seven_app.model.User;
import com.example.seven_app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Profile("init")
@Slf4j
@Order(2)
public class AuthorInitializer implements CommandLineRunner {

    private final UserRepository userRepository;

    @Value("${app.default-author-username}")
    private String defaultAuthorUsername;

    @Value("${app.default-author-usermail}")
    private String defaultAuthorUsermail;

    @Override
    public void run(String... args) {
        userRepository.findByUsernameOrEmail(defaultAuthorUsername, defaultAuthorUsermail)
                .switchIfEmpty(Mono.defer(() -> {
                    log.info("Default author user '{}' not found. Creating...", defaultAuthorUsername);
                    User defaultAuthor = new User();
                    defaultAuthor.setUsername(defaultAuthorUsername);
                    defaultAuthor.setEmail(defaultAuthorUsermail);
                    return userRepository.save(defaultAuthor);
                }))
                .doOnSuccess(user -> log.info("Default author user '{}' is present in the database with ID: {}", user.getUsername(), user.getId()))
                .block();
        log.info(">>>>>>>>> AuthorInitializer FINISHED. <<<<<<<<<");
    }
}
