package com.example.config;

import com.example.domain.ports.output.PaymentPort;
import com.example.domain.ports.output.OrderRepositoryPort;
import com.example.domain.service.OrderService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class BeanConfig {
    
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public OrderService orderService(
            OrderRepositoryPort repository,
            PaymentPort paymentPort) {
        return new OrderService(repository, paymentPort);
    }
}