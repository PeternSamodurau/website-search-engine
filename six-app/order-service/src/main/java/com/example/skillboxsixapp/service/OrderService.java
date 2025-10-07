package com.example.skillboxsixapp.service;

import com.example.skillboxsixapp.OrderDTO;

public interface OrderService {

    /**
     * Создает новый заказ на основе входящих данных,
     * формирует событие и отправляет его в Kafka.
     *
     * @param productName Название продукта.
     * @param quantity    Количество.
     * @return Созданный объект OrderDTO с присвоенным ID и датой.
     */
    OrderDTO createOrder(String productName, int quantity);
}