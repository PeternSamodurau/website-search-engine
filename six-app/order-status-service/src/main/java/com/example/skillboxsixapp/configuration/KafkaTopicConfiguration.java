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
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderStatusTopic() {
        return TopicBuilder.name("order-status-topic")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
