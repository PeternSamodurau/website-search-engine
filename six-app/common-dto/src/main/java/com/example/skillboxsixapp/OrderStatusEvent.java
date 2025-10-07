package com.example.skillboxsixapp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusEvent {
    private UUID eventId;
    private OrderDTO order;
    private Status status;
    private LocalDateTime eventDate;
}