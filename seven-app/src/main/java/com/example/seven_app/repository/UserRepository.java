package com.example.seven_app.repository;

import com.example.seven_app.model.User;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface UserRepository extends ReactiveMongoRepository<User, String> {

    Mono<User> findByUsernameOrEmail(String username, String email);
    Mono<User> findByUsername(String username);
}
