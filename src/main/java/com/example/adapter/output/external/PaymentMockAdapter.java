package com.example.adapter.output.external;

import com.example.domain.ports.output.PaymentPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
@ConditionalOnProperty(name = "payment.mock.enabled", havingValue = "true")
public class PaymentMockAdapter implements PaymentPort {

    @Override
    public boolean process(String orderId, BigDecimal amount) {
        System.out.println("🔄 [MOCK] Processing payment: " + orderId + " - USD " + amount);
        return true;
    }
}