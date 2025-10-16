package com.example.seven_app.init;

import com.example.seven_app.model.User;
import com.example.seven_app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
@RequiredArgsConstructor
@Profile("init")
@Slf4j
@Order(1) // Highest priority
public class UserInitializer implements CommandLineRunner {

    private final UserRepository userRepository;

    @Override
    public void run(String... args) {
        userRepository.count()
                .flatMap(count -> {
                    if (count == 0) {
                        log.info(">>>>>>>>> EXECUTING NEW CODE! Creating 5 test users... <<<<<<<<<");
                        return Flux.range(1, 5)
                                .flatMap(i -> {
                                    User user = new User();
                                    user.setUsername("Test User " + i);
                                    user.setEmail("testuser" + i + "@example.com");
                                    return userRepository.save(user);
                                })
                                .collectList()
                                .doOnSuccess(users -> log.info("Successfully created {} test users.", users.size()));
                    } else {
                        log.info("{} users already present in the database. Skipping creation.", count);
                        return Flux.empty().collectList(); // Return an empty list publisher
                    }
                })
                .block(); // This is safe and necessary on startup
        log.info(">>>>>>>>> UserInitializer FINISHED. <<<<<<<<<");
    }
}
