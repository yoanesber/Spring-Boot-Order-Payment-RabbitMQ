package com.yoanesber.order_payment_rabbitmq.listener;

import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.retry.RetryContext;
import org.springframework.retry.support.RetrySynchronizationManager;
import org.springframework.stereotype.Component;

import com.yoanesber.order_payment_rabbitmq.util.HelperUtil;

/**
 * PaymentListener is a component that listens for messages from RabbitMQ queues related to order payment processing.
 * It handles both successful and failed payment messages.
 * The class uses RabbitMQ's @RabbitListener annotation to define methods that will be triggered when messages are received.
 */

@Component
public class PaymentListener {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final boolean isSimulated = false; // Simulate processing failure for demonstration purposes 

    @RabbitListener(queues = "order.payment.success.queue", containerFactory = "successQueueFactory")
    public void handleSuccess(Message message) throws Exception {
        // Get the message body as a Map
        Map<String, Object> messageMap = HelperUtil.convertToMap(message.getBody());
        
        // Get the retry count from the RetrySynchronizationManager
        int retryCount = Optional.ofNullable(RetrySynchronizationManager.getContext())
                             .map(RetryContext::getRetryCount)
                             .orElse(0);

        if (retryCount > 0) {
            logger.info("Retrying message processing. Retry count: {} with message: {}", retryCount, messageMap.toString());
        } else {
            logger.info("Processing message for the first time. Message: {}", messageMap.toString());
        }

        // Process the message
        // For example, update the order status in the database or send a notification

        // Simulate processing failure for demonstration purposes
        if (isSimulated) {
            logger.info("Simulating processing failure for demonstration purposes...");
            throw new RuntimeException("Simulated processing failure...");
        }
    }

    @RabbitListener(queues = "order.payment.failed.queue", containerFactory = "failedQueueFactory")
    public void handleFailed(Message message) throws Exception {
        // Get the message body as a Map
        Map<String, Object> messageMap = HelperUtil.convertToMap(message.getBody());
        
        // Get the retry count from the RetrySynchronizationManager
        int retryCount = Optional.ofNullable(RetrySynchronizationManager.getContext())
                             .map(RetryContext::getRetryCount)
                             .orElse(0);

        if (retryCount > 0) {
            logger.info("Retrying message processing. Retry count: {} with message: {}", retryCount, messageMap.toString());
        } else {
            logger.info("Processing message for the first time. Message: {}", messageMap.toString());
        }

        // Process the message
        // For example, log the error or send an alert

        // Simulate processing failure for demonstration purposes
        if (isSimulated) {
            logger.info("Simulating processing failure for demonstration purposes...");
            throw new RuntimeException("Simulated processing failure...");
        }
    }
}
