package com.example.skillboxsixapp.configuration;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfiguration {

    @Bean
    public NewTopic orderTopic() {
        return TopicBuilder.name("order-topic")
                .partitions(1) // Для многопоточной обработки, 3 партиции.
                .replicas(1)   // Реплика всегда 1, т.к. у нас 1 брокер
                .build();
    }
}
