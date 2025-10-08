package com.example.skillboxsixapp.listener;

import com.example.skillboxsixapp.OrderStatusEvent;
import com.example.skillboxsixapp.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderListener {

    private final KafkaTemplate<String, OrderStatusEvent> kafkaTemplate;

    private static final String ORDER_TOPIC = "order-topic";
    private static final String ORDER_STATUS_TOPIC = "order-status-topic";

    @KafkaListener(topics = ORDER_TOPIC, groupId = "order-status-group")
    public void handleOrder(OrderStatusEvent event) {
        log.info("Received message from {}: {}", ORDER_TOPIC, event);

        // Имитируем обработку заказа и меняем его статус
        event.setStatus(Status.COMPLETED);

        log.info("Sending updated status to {}: {}", ORDER_STATUS_TOPIC, event);

        // Отправляем событие с обновленным статусом в другой топик
        kafkaTemplate.send(ORDER_STATUS_TOPIC, event.getOrder().getOrderId().toString(), event);
    }
}
