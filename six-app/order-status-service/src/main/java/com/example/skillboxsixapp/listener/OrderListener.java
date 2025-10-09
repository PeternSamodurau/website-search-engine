package com.example.skillboxsixapp.listener;

import com.example.skillboxsixapp.OrderStatusEvent;
import com.example.skillboxsixapp.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderListener {

    private final KafkaTemplate<String, OrderStatusEvent> kafkaTemplate;

    @KafkaListener(topics = "order-topic", groupId = "order-status-group",concurrency = "1") // поменять на 3 для многопоточности
    public void handleOrder(ConsumerRecord<String, OrderStatusEvent> record) {

        log.info("Received message from topic '{}': {}", record.topic(), record.value());
        log.info("Key: {}; Partition: {}; Topic: {}, Timestamp: {}", record.key(), record.partition(), record.topic(), record.timestamp());

        OrderStatusEvent event = record.value();
        event.setStatus(Status.COMPLETED);
        event.setEventDate(LocalDateTime.now());

        kafkaTemplate.send("order-status-topic", event.getEventId().toString(), event);
        log.info("Sending updated status to order-status-topic: {}", event);
    }
}