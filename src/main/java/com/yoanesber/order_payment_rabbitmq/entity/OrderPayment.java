package com.yoanesber.order_payment_rabbitmq.entity;

import java.math.BigDecimal;
import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class OrderPayment {
    private Long id;
    private String orderId; // Order identifier (linked to Orders table)
    private BigDecimal amount; // Payment amount
    private String currency; // e.g., USD, EUR
    private String paymentMethod; // e.g., CREDIT_CARD, PAYPAL, BANK_TRANSFER
    private String paymentStatus; // PENDING, SUCCESS, FAILED
    private String cardNumber; // Credit card number, e.g., 1234 5678 9012 3456
    private String cardExpiry; // Credit card expiry date, e.g., MM/YY
    private String cardCvv; // Credit card CVV (Card Verification Value), e.g., 123
    private String paypalEmail; // PayPal email address
    private String bankAccount; // Bank account number, e.g., 1234567890
    private String bankName; // Bank name, e.g., Bank of Indonesia
    private String transactionId; // Reference from payment gateway
    private int retryCount = 0; // Number of retries in case of payment failure
    private Instant createdAt = Instant.now(); // Payment creation time
    private Instant updatedAt = Instant.now(); // Payment update time
}
