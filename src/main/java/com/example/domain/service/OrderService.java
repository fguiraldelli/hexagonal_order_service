package com.example.domain.service;

import com.example.domain.model.Order;
import com.example.domain.ports.input.CreateOrderPort;
import com.example.domain.ports.input.ConfirmOrderPort;
import com.example.domain.ports.output.OrderRepositoryPort;
import com.example.domain.ports.output.PaymentPort;
import lombok.RequiredArgsConstructor;
import java.math.BigDecimal;

@RequiredArgsConstructor
public class OrderService implements CreateOrderPort, ConfirmOrderPort {
    private final OrderRepositoryPort repository;
    private final PaymentPort paymentPort;

    @Override
    public Order create(String clientId, BigDecimal total) {
        Order newOrder = Order.create(clientId, total);
        return repository.save(newOrder);
    }

    @Override
    public Order confirm(String orderId) {
        Order order = repository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Order not found with id: " + orderId));

        boolean paymentApproved = paymentPort.process(orderId, order.getTotal());
        if (!paymentApproved) {
            throw new RuntimeException("Payment was declined");
        }

        Order confirmedOrder = order.confirm();
        return repository.save(confirmedOrder);
    }
}