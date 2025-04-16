package com.yoanesber.order_payment_rabbitmq.service.impl;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.yoanesber.order_payment_rabbitmq.dto.CreateOrderPaymentRequestDTO;
import com.yoanesber.order_payment_rabbitmq.dto.PaymentBankRequestDTO;
import com.yoanesber.order_payment_rabbitmq.dto.PaymentCCRequestDTO;
import com.yoanesber.order_payment_rabbitmq.dto.PaymentPaypalRequestDTO;
import com.yoanesber.order_payment_rabbitmq.dto.PaymentResponseDTO;
import com.yoanesber.order_payment_rabbitmq.entity.CustomException;
import com.yoanesber.order_payment_rabbitmq.entity.Order;
import com.yoanesber.order_payment_rabbitmq.entity.OrderDetail;
import com.yoanesber.order_payment_rabbitmq.entity.OrderPayment;
import com.yoanesber.order_payment_rabbitmq.publisher.MessagePublisher;
import com.yoanesber.order_payment_rabbitmq.service.OrderPaymentService;

/**
 * OrderPaymentServiceImpl is a service class that handles the creation of order payments.
 * It validates the payment request, processes the payment through different methods (credit card, PayPal, bank transfer),
 * and publishes the payment result to RabbitMQ.
 */

@Service
public class OrderPaymentServiceImpl implements OrderPaymentService {

    // private static final String SUCCESS_QUEUE = "payment.success.queue";
    // private static final String FAILED_QUEUE = "payment.failed.queue";

    @Value("${spring.rabbitmq.order-payment.exchange-name}")
    private String paymentExchangeName;

    @Value("${spring.rabbitmq.order-payment.payment-success-routing-key}")
    private String paymentSuccessRoutingKey;

    @Value("${spring.rabbitmq.order-payment.payment-failed-routing-key}")
    private String paymentFailedRoutingKey;

    private final MessagePublisher messagePublisher;

    public OrderPaymentServiceImpl(MessagePublisher messagePublisher) {
        this.messagePublisher = messagePublisher;
    }

    private Order getOrderByID (String orderId) {
        // Simulate fetching an order from the database
        // For simplicity, we will create a new Order object with dummy data
        Order order = new Order();
        // Unique Order ID
        order.setOrderId(orderId);

        // Order Date (current timestamp)
        order.setOrderDate(LocalDateTime.now());

        // Order Status
        order.setOrderStatus("PENDING");

        // Order Total (e.g., total price of items)
        order.setOrderTotal(new BigDecimal("199.99"));

        // Currency
        order.setCurrency("IDR");

        // Customer Information
        order.setCustomerId("CUST1001");
        order.setCustomerName("Agus Yulianto");
        order.setCustomerEmail("agus_yulianto@example.com");
        order.setCustomerPhone("+62-811-222-3333");

        // Payment Information
        order.setPaymentMethod("CREDIT_CARD");
        order.setPaymentStatus("PENDING_PAYMENT");

        // Shipping Information
        order.setShippingAddress("Jl. Melati V No. 8, Solo, Jawa Tengah, Indonesia");
        order.setShippingMethod("STANDARD");
        order.setDeliveryDate(LocalDateTime.now().plusDays(5)); // Expected delivery in 5 days

        // Tax and Discount
        order.setTaxAmount(new BigDecimal("9.99"));
        order.setDiscountCode("DISCOUNT10");
        order.setDiscountAmount(new BigDecimal("10.00"));

        // Metadata
        order.setCreatedAt(Instant.now());
        order.setUpdatedAt(Instant.now());
        order.setProcessedBy("AdminUser");

        // Order Details (list of items in the order)
        // For simplicity, we will add a single item
        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setProductId("PROD1001");
        orderDetail.setProductName("Product A");
        orderDetail.setProductPrice(new BigDecimal("99.99"));
        orderDetail.setQuantity(2);
        orderDetail.setSubtotal(orderDetail.getProductPrice().multiply(new BigDecimal(orderDetail.getQuantity())));
        orderDetail.setDiscountAmount(new BigDecimal("10.00"));
        orderDetail.setTotalPrice(orderDetail.getSubtotal().subtract(orderDetail.getDiscountAmount()));
        orderDetail.setProductImageUrl("https://example.com/product-a.jpg");
        orderDetail.setNotes("No special notes");

        // Set the order details
        order.setOrderDetails(List.of(orderDetail));

        return order;
    }

    private void validateOrderPayment(CreateOrderPaymentRequestDTO orderPaymentDTO) {
        try {
            // Validate the order payment request
            Assert.notNull(orderPaymentDTO, "OrderPaymentDTO must not be null");
            Assert.notNull(orderPaymentDTO.getOrderId(), "Order ID must not be null");
            Assert.notNull(orderPaymentDTO.getAmount(), "Amount must not be null");
            Assert.isTrue(orderPaymentDTO.getAmount().compareTo(BigDecimal.ZERO) > 0, "Amount must be greater than zero");
            Assert.notNull(orderPaymentDTO.getCurrency(), "Currency must not be null");
            Assert.notNull(orderPaymentDTO.getPaymentMethod(), "Payment method must not be null");

            // Additional validation checks can be added here

        } catch (IllegalArgumentException e) {
            String errorMessage = "Validation failed for order payment: " + e.getMessage();

            // Publish a message to the failed queue
            messagePublisher.publish(paymentExchangeName, paymentFailedRoutingKey, 
                new CustomException(errorMessage));
            
            // Throw an exception if the payment method is invalid
            throw new IllegalArgumentException(errorMessage);
        }

        if (orderPaymentDTO.getPaymentMethod().equalsIgnoreCase("CREDIT_CARD")) {
            try {
                Assert.notNull(orderPaymentDTO.getCardNumber(), "Card number must not be null");
                Assert.notNull(orderPaymentDTO.getCardExpiry(), "Card expiry date must not be null");
                Assert.notNull(orderPaymentDTO.getCardCvv(), "Card CVV must not be null");
            } catch (IllegalArgumentException e) {
                String errorMessage = "Validation failed for credit card payment: " + e.getMessage();

                // Publish a message to the failed queue
                messagePublisher.publish(paymentExchangeName, paymentFailedRoutingKey, 
                    new CustomException(errorMessage));
                
                // Throw an exception if the payment method is invalid
                throw new IllegalArgumentException(errorMessage);
            }
        } else if (orderPaymentDTO.getPaymentMethod().equalsIgnoreCase("PAYPAL")) {
            try {
                Assert.notNull(orderPaymentDTO.getPaypalEmail(), "PayPal email must not be null");
            } catch (IllegalArgumentException e) {
                String errorMessage = "Validation failed for PayPal payment: " + e.getMessage();

                // Publish a message to the failed queue
                messagePublisher.publish(paymentExchangeName, paymentFailedRoutingKey, 
                    new CustomException(errorMessage));
                
                // Throw an exception if the payment method is invalid
                throw new IllegalArgumentException(errorMessage);
            }
        } else if (orderPaymentDTO.getPaymentMethod().equalsIgnoreCase("BANK_TRANSFER")) {
            try {
                Assert.notNull(orderPaymentDTO.getBankAccount(), "Bank account must not be null");
                Assert.notNull(orderPaymentDTO.getBankName(), "Bank name must not be null");
            } catch (IllegalArgumentException e) {
                String errorMessage = "Validation failed for bank transfer payment: " + e.getMessage();

                // Publish a message to the failed queue
                messagePublisher.publish(paymentExchangeName, paymentFailedRoutingKey, 
                    new CustomException(errorMessage));
                
                // Throw an exception if the payment method is invalid
                throw new IllegalArgumentException(errorMessage);
            }
        } else {
            String errorMessage = "Invalid payment method: " + orderPaymentDTO.getPaymentMethod();

            // Publish a message to the failed queue
            messagePublisher.publish(paymentExchangeName, paymentFailedRoutingKey, 
                new CustomException(errorMessage));

            // Throw an exception if the payment method is invalid
            throw new IllegalArgumentException(errorMessage);
        }

        // Check if the order exists
        Order order = this.getOrderByID(orderPaymentDTO.getOrderId());
        if (order == null) {
            String errorMessage = "Order not found: " + orderPaymentDTO.getOrderId();

            // Publish a message to the failed queue
            messagePublisher.publish(paymentExchangeName, paymentFailedRoutingKey, 
                new CustomException(errorMessage));

            // Throw an exception if the order is not found
            throw new IllegalArgumentException("Order not found: " + orderPaymentDTO.getOrderId());
        }

        // Check if the order payment status is PENDING_PAYMENT
        if (!order.getPaymentStatus().equalsIgnoreCase("PENDING_PAYMENT")) {
            String errorMessage = "Order payment status is not PENDING_PAYMENT: " + order.getPaymentStatus();
            
            // Publish a message to the failed queue
            messagePublisher.publish(paymentExchangeName, paymentFailedRoutingKey, 
                new CustomException(errorMessage));

            // Throw an exception if the payment status is not PENDING_PAYMENT
            throw new IllegalArgumentException(errorMessage);
        }

        // Check if the payment amount matches the order total
        if (order.getOrderTotal().compareTo(orderPaymentDTO.getAmount()) != 0) {
            String errorMessage = "Payment amount does not match order total: " + order.getOrderTotal() + " != " + orderPaymentDTO.getAmount();

            // Publish a message to the failed queue
            messagePublisher.publish(paymentExchangeName, paymentFailedRoutingKey, 
                new CustomException(errorMessage));

            // Throw an exception if the payment amount does not match the order total
            throw new IllegalArgumentException(errorMessage);
        }

        // Additional validation checks can be added here

    }

    private PaymentResponseDTO processPaymentWithCC(PaymentCCRequestDTO paymentCCRequestDTO) {
        Assert.notNull(paymentCCRequestDTO, "PaymentCCRequestDTO must not be null");

        // Call the credit card payment gateway API
        try {
            // Simulate processing the payment
            Thread.sleep(2000); // Simulate a delay of 2 seconds

            // For simplicity, we will generate a random transaction ID
            String transactionId = "TXN" + System.currentTimeMillis();
            String paymentStatus = "SUCCESS"; // Assume payment is successful

            // Check if the transaction ID is empty
            // Payment status can be "SUCCESS" or "FAILED"; If failed, run scheduled job to retry payment
            if (transactionId == null || transactionId.isEmpty()) {
                paymentStatus = "FAILED";
            }

            return new PaymentResponseDTO(transactionId, paymentStatus);
        } catch (InterruptedException e) {
            // Publish a message to the failed queue
            messagePublisher.publish(paymentExchangeName, paymentFailedRoutingKey, 
                new CustomException("Error processing credit card payment for order " + paymentCCRequestDTO.getOrderId() + ": " + e.getMessage()));

            // Return null to indicate failure
            return null;
        }
    }

    private PaymentResponseDTO processPaymentWithPaypal(PaymentPaypalRequestDTO paymentPaypalRequestDTO) {
        Assert.notNull(paymentPaypalRequestDTO, "PaymentPaypalRequestDTO must not be null");

        // Call the PayPal payment gateway API
        try {
            // Simulate processing the payment
            Thread.sleep(2000); // Simulate a delay of 2 seconds

            // For simplicity, we will generate a random transaction ID
            String transactionId = "TXN" + System.currentTimeMillis();
            String paymentStatus = "SUCCESS"; // Assume payment is successful

            // Check if the transaction ID is empty
            // Payment status can be "SUCCESS" or "FAILED"; If failed, run scheduled job to retry payment
            if (transactionId == null || transactionId.isEmpty()) {
                paymentStatus = "FAILED";
            }

            return new PaymentResponseDTO(transactionId, paymentStatus);
        } catch (InterruptedException e) {
            // Publish a message to the failed queue
            messagePublisher.publish(paymentExchangeName, paymentFailedRoutingKey, 
                new CustomException("Error processing PayPal payment for order " + paymentPaypalRequestDTO.getOrderId() + ": " + e.getMessage()));

            // Return null to indicate failure
            return null;
        }
    }

    private PaymentResponseDTO processPaymentWithBank(PaymentBankRequestDTO paymentBankRequestDTO) {
        Assert.notNull(paymentBankRequestDTO, "PaymentBankRequestDTO must not be null");

        // Call the bank transfer payment gateway API
        try {
            // Simulate processing the payment
            Thread.sleep(2000); // Simulate a delay of 2 seconds

            // For simplicity, we will generate a random transaction ID
            String transactionId = "TXN" + System.currentTimeMillis();
            String paymentStatus = "SUCCESS"; // Assume payment is successful

            // Check if the transaction ID is empty
            // Payment status can be "SUCCESS" or "FAILED"; If failed, run scheduled job to retry payment
            if (transactionId == null || transactionId.isEmpty()) {
                paymentStatus = "FAILED";
            }

            return new PaymentResponseDTO(transactionId, paymentStatus);
        } catch (InterruptedException e) {
            // Publish a message to the failed queue
            messagePublisher.publish(paymentExchangeName, paymentFailedRoutingKey, 
                new CustomException("Error processing bank transfer payment for order " + paymentBankRequestDTO.getOrderId() + ": " + e.getMessage()));

            // Return null to indicate failure
            return null;
        }
    }

    private PaymentResponseDTO processPayment(CreateOrderPaymentRequestDTO orderPaymentDTO) {
        Assert.notNull(orderPaymentDTO, "OrderPaymentDTO must not be null");

        if (orderPaymentDTO.getPaymentMethod().equalsIgnoreCase("CREDIT_CARD")) {
            return processPaymentWithCC(new PaymentCCRequestDTO(orderPaymentDTO.getOrderId(), 
                orderPaymentDTO.getAmount(), 
                orderPaymentDTO.getCurrency(),
                orderPaymentDTO.getCardNumber(),
                orderPaymentDTO.getCardExpiry(),
                orderPaymentDTO.getCardCvv()));
        } else if (orderPaymentDTO.getPaymentMethod().equalsIgnoreCase("PAYPAL")) {
            return processPaymentWithPaypal(new PaymentPaypalRequestDTO(orderPaymentDTO.getOrderId(), 
                orderPaymentDTO.getAmount(), 
                orderPaymentDTO.getCurrency(),
                orderPaymentDTO.getPaypalEmail()));
        } else if (orderPaymentDTO.getPaymentMethod().equalsIgnoreCase("BANK_TRANSFER")) {
            return processPaymentWithBank(new PaymentBankRequestDTO(orderPaymentDTO.getOrderId(), 
                orderPaymentDTO.getAmount(), 
                orderPaymentDTO.getCurrency(),
                orderPaymentDTO.getBankAccount(),
                orderPaymentDTO.getBankName()));
        } else {
            return null; // Invalid payment method
        }
    }

    @Override
    public OrderPayment createOrderPayment(CreateOrderPaymentRequestDTO orderPaymentDTO) {
        Assert.notNull(orderPaymentDTO, "OrderPaymentDTO must not be null");
        
        // Validate request (check order exists, amount is valid, etc.)
        this.validateOrderPayment(orderPaymentDTO);
        
        // Call the payment gateway API and get the transaction details
        String paymentStatus = "FAILED"; // Default to FAILED
        String transactionId = "";
        PaymentResponseDTO paymentResponse = this.processPayment(orderPaymentDTO);

        // Check if the payment response is null (indicating a failure)
        if (paymentResponse == null) {
            String errorMessage = "Payment processing failed for order " + orderPaymentDTO.getOrderId() + ": Payment response is null";

            // Publish a message to the failed queue
            messagePublisher.publish(paymentExchangeName, paymentFailedRoutingKey, 
                new CustomException(errorMessage));
                
            // Throw an exception if the payment response is null
            throw new IllegalArgumentException(errorMessage);
        }

        // Extract the transaction ID and payment status from the response
        transactionId = paymentResponse.getTransactionId();
        paymentStatus = paymentResponse.getPaymentStatus();

        // Check if the payment status is "FAILED"
        if (paymentStatus.equalsIgnoreCase("FAILED") || transactionId == null || transactionId.isEmpty()) {
            String errorMessage = "Payment processing failed for order " + orderPaymentDTO.getOrderId() + ": Payment status is FAILED or transaction ID is empty";

            // Publish a message to the failed queue
            messagePublisher.publish(paymentExchangeName, paymentFailedRoutingKey, 
                new CustomException(errorMessage));

            // Throw an exception if the payment status is FAILED or transaction ID is empty
            throw new IllegalArgumentException(errorMessage);
        }

        // Create an OrderPayment entity
        OrderPayment orderPayment = new OrderPayment();
        orderPayment.setId(Long.valueOf(System.currentTimeMillis())); // Simulate ID generation
        orderPayment.setOrderId(orderPaymentDTO.getOrderId());
        orderPayment.setAmount(orderPaymentDTO.getAmount());
        orderPayment.setCurrency(orderPaymentDTO.getCurrency());
        orderPayment.setPaymentMethod(orderPaymentDTO.getPaymentMethod());
        orderPayment.setPaymentStatus(paymentStatus);

        if (orderPaymentDTO.getPaymentMethod().equalsIgnoreCase("CREDIT_CARD")) {
            orderPayment.setCardNumber(orderPaymentDTO.getCardNumber());
            orderPayment.setCardExpiry(orderPaymentDTO.getCardExpiry());
            orderPayment.setCardCvv(orderPaymentDTO.getCardCvv());
        } else if (orderPaymentDTO.getPaymentMethod().equalsIgnoreCase("PAYPAL")) {
            orderPayment.setPaypalEmail(orderPaymentDTO.getPaypalEmail());
        } else if (orderPaymentDTO.getPaymentMethod().equalsIgnoreCase("BANK_TRANSFER")) {
            orderPayment.setBankAccount(orderPaymentDTO.getBankAccount());
            orderPayment.setBankName(orderPaymentDTO.getBankName());
        }

        orderPayment.setTransactionId(transactionId);
        orderPayment.setCreatedAt(Instant.now());
        orderPayment.setUpdatedAt(Instant.now());

        // Save the OrderPayment entity to the database
        
        // Publish the order payment event to RabbitMQ
        messagePublisher.publish(paymentExchangeName, paymentSuccessRoutingKey, orderPayment);

        // For simplicity, we will return the OrderPayment object directly
        return orderPayment;
    }
}
