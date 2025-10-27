package com.example.domain.ports.output;

import java.math.BigDecimal;

public interface PaymentPort {
    boolean process(String orderId, BigDecimal amount);
}