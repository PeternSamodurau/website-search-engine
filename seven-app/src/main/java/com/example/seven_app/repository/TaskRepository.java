package com.example.seven_app.repository;

import com.example.seven_app.model.Task;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface TaskRepository extends ReactiveMongoRepository<Task, String> {
    Mono<Boolean> existsByNameAndDescription(String name, String description);
}
