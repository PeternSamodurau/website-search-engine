package com.example.skillboxsixapp.service.impl;

import com.example.skillboxsixapp.OrderDTO;
import com.example.skillboxsixapp.OrderStatusEvent;
import com.example.skillboxsixapp.Status;
import com.example.skillboxsixapp.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final KafkaTemplate<String, OrderStatusEvent> kafkaTemplate;

    @Override
    public OrderDTO createOrder(String productName, int quantity) {

        OrderDTO order = new OrderDTO(
                UUID.randomUUID(),
                productName,
                quantity,
                LocalDateTime.now()
        );
        log.info("Created OrderDTO: {}", order);

        // 2. Создаем событие-конверт для отправки в Kafka
        OrderStatusEvent event = new OrderStatusEvent(
                UUID.randomUUID(),
                order,
                Status.CREATED, // Устанавливаем первоначальный статус
                LocalDateTime.now()
        );
        log.info("Created OrderStatusEvent: {}", event);

        // 3. Отправляем событие в топик 'order-topic'
        // В качестве ключа сообщения используем ID заказа для правильного распределения по партициям
        kafkaTemplate.send("order-topic", order.getOrderId().toString(), event);
        log.info("Sent event to Kafka topic 'order-topic' with key: {}", order.getOrderId());

        // 4. Возвращаем созданный DTO контроллеру, как и требует интерфейс
        return order;
    }
}