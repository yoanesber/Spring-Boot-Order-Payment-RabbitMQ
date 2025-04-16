package com.yoanesber.order_payment_rabbitmq.dto;

import java.math.BigDecimal;
import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Getter
@Setter
@NoArgsConstructor // Required for Jackson deserialization when receiving JSON requests.
@AllArgsConstructor // Helps create DTO objects easily (useful when converting from entities).
public class CreateOrderPaymentResponseDTO {
    private String orderId; // Order identifier (linked to Orders table)
    private String transactionId; // Reference from payment gateway
    private String paymentStatus; // PENDING, SUCCESS, FAILED
    private BigDecimal amount; // Payment amount
    private String currency; // e.g., USD, EUR
    private String paymentMethod; // e.g., CREDIT_CARD, PAYPAL, BANK_TRANSFER
    private Instant createdAt; // Payment creation time
}
