package com.example.domain.ports.input;

import com.example.domain.model.Order;

public interface ConfirmOrderPort {
    Order confirm(String orderId);
}