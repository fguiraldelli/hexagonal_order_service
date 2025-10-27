package com.example.adapter.output.persistence;

import com.example.domain.model.Order;
import com.example.domain.model.OrderStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderEntity {

    @Id
    private UUID id;

    @Column(name = "client_id", nullable = false)
    private String clientId;

    @Column(name = "total", nullable = false)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static OrderEntity from(Order order) {
        return new OrderEntity(
            order.getId(),
            order.getClientId(),
            order.getTotal(),
            order.getStatus(),
            order.getCreatedAt()
        );
    }

    public Order toDomain() {
        return new Order(
            this.id,
            this.clientId,
            this.total,
            this.status,
            this.createdAt
        );
    }
}