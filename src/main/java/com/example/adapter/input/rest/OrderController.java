package com.example.adapter.input.rest;

import com.example.domain.ports.input.CreateOrderPort;
import com.example.domain.ports.input.ConfirmOrderPort;
import com.example.domain.model.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {
    private final CreateOrderPort createOrderPort;
    private final ConfirmOrderPort confirmOrderPort;

    @PostMapping
    public ResponseEntity<OrderResponse> create(@RequestBody CreateOrderRequest request) {
        Order order = createOrderPort.create(request.clientId(), request.total());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(OrderResponse.from(order));
    }

    @PutMapping("/{id}/confirm")
    public ResponseEntity<OrderResponse> confirm(@PathVariable String id) {
        Order order = confirmOrderPort.confirm(id);
        return ResponseEntity.ok(OrderResponse.from(order));
    }

    record CreateOrderRequest(String clientId, BigDecimal total) {}
    
    record OrderResponse(String id, String clientId, BigDecimal total, String status) {
        static OrderResponse from(Order order) {
            return new OrderResponse(
                order.getId().toString(),
                order.getClientId(),
                order.getTotal(),
                order.getStatus().name()
            );
        }
    }
}