package com.yoanesber.order_payment_rabbitmq.publisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;  
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * MessagePublisher is a component responsible for publishing messages to RabbitMQ exchanges.
 * It uses RabbitTemplate to send messages and provides a method to publish messages with a specific exchange and routing key.
 */

@Component
public class MessagePublisher {
    
    private final RabbitTemplate rabbitTemplate;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public MessagePublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Publishes a message to the specified RabbitMQ exchange with the given routing key.
     * The message is wrapped in a Map with "event" and "message" keys.
     *
     * @param exchangeName The name of the RabbitMQ exchange to publish to.
     * @param routingKey The routing key for the message.
     * @param message    The message to be published.
     */
    public void publish(String exchangeName, String routingKey, Object message) {
        Assert.hasText(exchangeName, "Exchange name must not be empty");
        Assert.hasText(routingKey, "Routing key must not be empty");
        Assert.notNull(message, "Message must not be null");

        try {
            rabbitTemplate.convertAndSend(exchangeName, routingKey, message);
        } catch (AmqpException e) {
            logger.error("Failed to publish message to exchange: {}, routingKey: {}, message: {}. Error: {}", exchangeName, routingKey, message, e.getMessage(), e);
            throw new RuntimeException("Failed to publish message", e);
        } catch (Exception e) {
            logger.error("Failed to publish message to exchange: {}, routingKey: {}, message: {}. Error: {}", exchangeName, routingKey, message, e.getMessage(), e);
            throw new RuntimeException("Failed to publish message", e);
        } 
    }
}
