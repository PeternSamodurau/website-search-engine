package com.example.skillboxsixapp.controller;

import com.example.skillboxsixapp.OrderDTO;
import com.example.skillboxsixapp.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderDTO> createOrder(@Valid @RequestBody OrderDTO orderRequest) {

        log.info("Received request to create order: {}", orderRequest);

        String product = orderRequest.getProductName();
        Integer quantity = orderRequest.getQuantity();

        OrderDTO createdOrder = orderService.createOrder(product, quantity);

        log.info("Successfully created order: {}", createdOrder);

        return ResponseEntity.status(HttpStatus.CREATED).body(createdOrder);
    }
}