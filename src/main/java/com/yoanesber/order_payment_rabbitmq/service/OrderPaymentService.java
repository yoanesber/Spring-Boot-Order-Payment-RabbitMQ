package com.yoanesber.order_payment_rabbitmq.service;

import com.yoanesber.order_payment_rabbitmq.dto.CreateOrderPaymentRequestDTO;
import com.yoanesber.order_payment_rabbitmq.entity.OrderPayment;

public interface OrderPaymentService {
    // Create a new OrderPayment record.
    OrderPayment createOrderPayment(CreateOrderPaymentRequestDTO orderPaymentDTO);
}
