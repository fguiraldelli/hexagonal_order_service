package com.example.domain.model;

import lombok.AllArgsConstructor;
import lombok.Value;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Value
@AllArgsConstructor
public class Order {
    UUID id;
    String clientId;
    BigDecimal total;
    OrderStatus status;
    LocalDateTime createdAt;

    public static Order create(String clientId, BigDecimal total) {
        return new Order(
            UUID.randomUUID(),
            clientId,
            total,
            OrderStatus.PENDING,
            LocalDateTime.now()
        );
    }

    public Order confirm() {
        if (this.status != OrderStatus.PENDING) {
            throw new IllegalStateException("Order cannot be confirmed in status: " + status);
        }
        return new Order(id, clientId, total, OrderStatus.CONFIRMED, createdAt);
    }
}