package com.example.adapter.output.persistence;

import com.example.domain.model.Order;
import com.example.domain.ports.output.OrderRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OrderJpaAdapter implements OrderRepositoryPort {
    private final OrderSpringJpaRepository jpaRepository;

    @Override
    public Order save(Order order) {
        OrderEntity entity = OrderEntity.from(order);
        OrderEntity saved = jpaRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<Order> findById(String id) {
        return jpaRepository.findById(UUID.fromString(id))
            .map(OrderEntity::toDomain);
    }

    @Override
    public void delete(String id) {
        jpaRepository.deleteById(UUID.fromString(id));
    }
}