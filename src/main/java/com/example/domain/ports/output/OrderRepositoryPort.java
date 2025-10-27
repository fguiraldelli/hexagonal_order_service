package com.example.domain.ports.output;

import com.example.domain.model.Order;
import java.util.Optional;

public interface OrderRepositoryPort {
    Order save(Order order);
    Optional<Order> findById(String id);
    void delete(String id);
}