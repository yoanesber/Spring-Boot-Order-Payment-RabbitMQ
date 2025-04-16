package com.yoanesber.order_payment_rabbitmq.dto;

import java.math.BigDecimal;

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
public class CreateOrderPaymentRequestDTO {
    private String orderId; // Order identifier (linked to Orders table)
    private BigDecimal amount; // Payment amount
    private String currency; // e.g., USD, EUR
    private String paymentMethod; // e.g., CREDIT_CARD, PAYPAL, BANK_TRANSFER
    
    // Credit card details
    private String cardNumber; // Credit card number
    private String cardExpiry; // Credit card expiry date
    private String cardCvv; // Credit card CVV (Card Verification Value)

    // PayPal details
    private String paypalEmail; // PayPal email address

    // Bank transfer details
    private String bankAccount; // Bank account number
    private String bankName; // Bank name
}
