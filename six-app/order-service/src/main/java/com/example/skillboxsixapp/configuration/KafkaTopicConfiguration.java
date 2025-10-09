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
                .partitions(3) // Например, 3 партиции. по умолчанию 1 партиция
                .replicas(1)   // Реплика всегда 1, т.к. у нас 1 брокер
                .build();
    }

    @Bean
    public NewTopic orderStatusTopic() {
        return TopicBuilder.name("order-status-topic")
                .partitions(3) // Например, 3 партиции. по умолчанию 1 партиция
                .replicas(1)
                .build();
    }
}
