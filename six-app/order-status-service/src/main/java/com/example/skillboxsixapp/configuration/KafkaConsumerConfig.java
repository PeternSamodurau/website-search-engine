package com.example.skillboxsixapp.configuration;

import com.example.skillboxsixapp.OrderStatusEvent; // <-- Я обновил импорт здесь для точности
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ConsumerFactory<String, OrderStatusEvent> orderStatusEventConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        JsonDeserializer<OrderStatusEvent> deserializer = new JsonDeserializer<>(OrderStatusEvent.class);
        deserializer.setRemoveTypeHeaders(false);
        deserializer.addTrustedPackages("*");
        deserializer.setUseTypeMapperForKey(true);

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderStatusEvent> orderStatusEventConcurrentKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, OrderStatusEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(orderStatusEventConsumerFactory());
        return factory;
    }
}