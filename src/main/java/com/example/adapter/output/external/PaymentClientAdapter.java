package com.example.adapter.output.external;

import com.example.domain.ports.output.PaymentPort;
import lombok.RequiredArgsConstructor;
import org.springframework.web.client.RestTemplate;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class PaymentClientAdapter implements PaymentPort {
    private final RestTemplate restTemplate;
    private static final String PAYMENT_SERVICE_URL = "http://localhost:8081/payments";

    @Override
    public boolean process(String orderId, BigDecimal amount) {
        try {
            PaymentRequest request = new PaymentRequest(orderId, amount);
            PaymentResponse response = restTemplate.postForObject(
                PAYMENT_SERVICE_URL,
                request,
                PaymentResponse.class
            );
            return response != null && response.approved();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    record PaymentRequest(String orderId, BigDecimal amount) {}
    record PaymentResponse(boolean approved) {}
}