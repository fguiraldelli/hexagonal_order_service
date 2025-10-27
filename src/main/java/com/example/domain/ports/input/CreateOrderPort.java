package com.example.domain.ports.input;

import com.example.domain.model.Order;
import java.math.BigDecimal;

public interface CreateOrderPort {
    Order create(String clientId, BigDecimal total);
}