package com.yoanesber.order_payment_rabbitmq.config;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/*
 * RabbitMQConfig is a configuration class for setting up RabbitMQ connections, exchanges, queues, and bindings.
 * It uses Spring AMQP to manage RabbitMQ resources and provides a RabbitTemplate for sending messages.
 * The configuration is loaded from application properties using @Value annotations.
 */

@Configuration
public class RabbitMQConfig {

    @Value("${spring.rabbitmq.host}")
    private String host;

    @Value("${spring.rabbitmq.port}")
    private int port;

    @Value("${spring.rabbitmq.username}")
    private String username;

    @Value("${spring.rabbitmq.password}")
    private String password;

    @Value("${spring.rabbitmq.virtual-host}")
    private String virtualHost;

    @Value("${spring.rabbitmq.publisher-returns}")
    private boolean publisherReturns;

    @Value("${spring.rabbitmq.channel-cache-size}")
    private int channelCacheSize;

    @Value("${spring.rabbitmq.connection-limit}")
    private int connectionLimit;

    @Value("${spring.rabbitmq.publisher-confirm-type}")
    private String publisherConfirmType;

    @Value("${spring.rabbitmq.requested-heart-beat}")
    private int requestedHeartBeat;

    @Value("${spring.rabbitmq.connection-timeout}")
    private int connectionTimeout;

    // Main exchange and queue configuration
    @Value("${spring.rabbitmq.order-payment.exchange-name}")
    private String paymentExchangeName;

    @Value("${spring.rabbitmq.order-payment.payment-success-queue-name}")
    private String paymentSuccessQueueName;

    @Value("${spring.rabbitmq.order-payment.payment-failed-queue-name}")
    private String paymentFailedQueueName;

    @Value("${spring.rabbitmq.order-payment.payment-success-routing-key}")
    private String paymentSuccessRoutingKey;

    @Value("${spring.rabbitmq.order-payment.payment-failed-routing-key}")
    private String paymentFailedRoutingKey;

    // Dead Letter Exchange (DLX) configuration
    @Value("${spring.rabbitmq.order-payment.exchange-dlx-name}")
    private String paymentExchangeDlxName;

    @Value("${spring.rabbitmq.order-payment.dlq-success-queue-name}")
    private String deadLetterQueueSuccessName;

    @Value("${spring.rabbitmq.order-payment.dlq-failed-queue-name}")
    private String deadLetterQueueFailedName;

    @Value("${spring.rabbitmq.order-payment.dlq-success-routing-key}")
    private String deadLetterRoutingKeySuccess;

    @Value("${spring.rabbitmq.order-payment.dlq-failed-routing-key}")
    private String deadLetterRoutingKeyFailed;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /*=== Payment Exchange Configuration ===
     * The payment exchange is used to route messages related to payment processing.
     * It is a topic exchange that allows for flexible routing based on the routing key.
     */
    @Bean
    public TopicExchange paymentExchange() {
        // Create a durable topic exchange for payment messages
        // The exchange will survive server restarts and messages will be routed based on the routing key
        return new TopicExchange(paymentExchangeName, true, false);
    }

    // === Payment Success Configuration ===
    @Bean
    public Queue paymentSuccessQueue() {
        // The queue will survive server restarts and messages will be retained until consumed
        // The queue is configured with a dead-letter exchange (DLX) and routing key for undeliverable messages
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", paymentExchangeDlxName);
        args.put("x-dead-letter-routing-key", deadLetterRoutingKeySuccess);
        return new Queue(paymentSuccessQueueName, true, false, false, args);
    }

    @Bean
    public Binding paymentSuccessBinding() {
        // Bind the payment success queue to the payment exchange with the specified routing key
        // This means that messages sent to the exchange with this routing key will be delivered to this queue
        return BindingBuilder.bind(paymentSuccessQueue()).to(paymentExchange()).with(paymentSuccessRoutingKey);
    }



    /*=== Payment Failed Configuration ===
     * The payment failed queue is used to handle messages that could not be processed successfully.
     * It is configured with a dead-letter exchange (DLX) and routing key for undeliverable messages.
     */
    @Bean
    public Queue paymentFailedQueue() {
        // The queue will survive server restarts and messages will be retained until consumed
        // The queue is configured with a dead-letter exchange (DLX) and routing key for undeliverable messages
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", paymentExchangeDlxName);
        args.put("x-dead-letter-routing-key", deadLetterRoutingKeyFailed);

        return new Queue(paymentFailedQueueName, true, false, false, args);
    }

    @Bean
    public Binding paymentFailedBinding() {
        // Bind the payment failed queue to the payment exchange with the specified routing key
        // This means that messages sent to the exchange with this routing key will be delivered to this queue
        return BindingBuilder.bind(paymentFailedQueue()).to(paymentExchange()).with(paymentFailedRoutingKey);
    }



    /*=== Dead Letter Exchange (DLX) Configuration ===
     * The DLX is used to handle undeliverable messages that cannot be routed to any queue.
     * Messages sent to the DLX will be routed to the specified dead letter queues based on the routing key.
     */
    @Bean
    public DirectExchange paymentDLXExchange() {
        // Create a durable dead letter exchange for undeliverable messages
        // The exchange will survive server restarts and messages will be routed based on the routing key
        return new DirectExchange(paymentExchangeDlxName, true, false);
    }

    @Bean
    public Queue paymentSuccessDLQ() {
        // Create a durable dead letter queue for undeliverable messages
        // The queue will survive server restarts and messages will be retained until consumed
        return new Queue(deadLetterQueueSuccessName, true);
    }

    @Bean
    public Queue paymentFailedDLQ() {
        // Create a durable dead letter queue for undeliverable messages
        // The queue will survive server restarts and messages will be retained until consumed
        return new Queue(deadLetterQueueFailedName, true);
    }

    @Bean
    public Binding paymentSuccessDLQBinding() {
        // Bind the dead letter queue for payment success to the dead letter exchange with the specified routing key
        // This means that messages sent to the DLX with this routing key will be delivered to this queue
        return BindingBuilder.bind(paymentSuccessDLQ()).to(paymentDLXExchange()).with(deadLetterRoutingKeySuccess);
    }

    @Bean
    public Binding paymentFailedDLQBinding() {
        // Bind the dead letter queue for payment failed to the dead letter exchange with the specified routing key
        // This means that messages sent to the DLX with this routing key will be delivered to this queue
        return BindingBuilder.bind(paymentFailedDLQ()).to(paymentDLXExchange()).with(deadLetterRoutingKeyFailed);
    }

    // Connection Factory and RabbitTemplate configuration
    @Bean
    public CachingConnectionFactory connectionFactory() {
        // Create a CachingConnectionFactory for RabbitMQ connections
        // This factory manages a pool of connections to RabbitMQ, allowing for efficient reuse of connections
        CachingConnectionFactory factory = new CachingConnectionFactory();
        factory.setHost(host);
        factory.setPort(port);
        factory.setUsername(username);
        factory.setPassword(password);
        factory.setVirtualHost(virtualHost);
        factory.setPublisherReturns(publisherReturns); // Enable publisher returns for undeliverable messages
        factory.setChannelCacheSize(channelCacheSize); // Size of the channel cache for RabbitMQ connections
        factory.setConnectionLimit(connectionLimit); // Limit the number of connections to RabbitMQ 
        factory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.valueOf(publisherConfirmType.toUpperCase())); // Set the publisher confirm type (SIMPLE or CORRELATED)
        factory.setRequestedHeartBeat(requestedHeartBeat); // Heartbeat interval in seconds, helps keep the connection alive
        factory.setConnectionTimeout(connectionTimeout); // Connection timeout in milliseconds, helps avoid long waits for connection attempts
        
        return factory;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(CachingConnectionFactory connectionFactory) {
        // Create a RabbitTemplate for sending messages to RabbitMQ exchanges and queues
        // The RabbitTemplate uses the connection factory to create connections and channels for sending messages
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);

        // The Jackson2JsonMessageConverter is used to convert messages to and from JSON format
        // This allows for easy serialization and deserialization of Java objects to JSON and vice versa
        rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());

        // To avoid deadlocked connections, it is generally recommended to use 
        //  a separate connection for publishers and consumers (except when a publisher is participating in a consumer transaction). 
        // Default 'false'. 
        // When setting this to true, be careful in that a RabbitAdmin that uses this template will declare queues on the publisher connection; 
        // this may not be what you expect, especially with exclusive queues that might be consumed in this application
        rabbitTemplate.setUsePublisherConnection(false);

        // Disable transactional mode on the channel.
        // This allows the use of publisher confirms (confirmSelect -> publisher-confirm-type=correlated) instead of transactions (txSelect),
        // which is more efficient and compatible with asynchronous message acknowledgments.
        rabbitTemplate.setChannelTransacted(false);

        // Set the mandatory flag when sending messages; only applies if a returnCallback had been provided
        // Set mandatory to true to ensure that messages are returned if they cannot be routed to any queue
        rabbitTemplate.setMandatory(true); 

        // This callback is invoked when a message cannot be routed to any queue
        // It provides information about the message and the reason for the failure
        rabbitTemplate.setReturnsCallback((returnedMessage) -> {
            int replyCode = returnedMessage.getReplyCode();

            switch (replyCode) {
                case 311:
                    logger.warn("Message size exceeds broker’s max-allowed size" +
                        " (e.g., message too large for the queue)." +
                        " This can happen when the message size exceeds the maximum allowed size for the queue.");
                    break;
                case 312:
                    logger.warn("No queue is bound to the exchange with the given routing key.");
                    break;
                case 313:
                    logger.warn("Message was returned because it required consumers, but none were available.");
                    break;
                case 320:
                    logger.warn("Broker closed the connection (e.g., due to shutdown, or management action)." +
                        " This can happen when the broker is shutting down or when a management action is performed.");
                    break;
                case 402:
                    logger.error("The client attempted to use an invalid virtual host or path." +
                        " This can happen when the virtual host or path is not configured correctly.");
                    break;
                case 403:
                    logger.error("Permission denied: missing ACL, tag, or vhost access." +
                    " This can happen when the user does not have permission to access the specified exchange or queue.");
                    break;
                case 404:
                    logger.error("Exchange/queue/routing key not found." +
                        " This can happen when the exchange or queue does not exist or is not configured correctly.");
                    break;
                case 405:
                    logger.error("The requested resource is locked (used by another connection)." +
                    " This can happen when a queue is declared as exclusive and another connection tries to access it.");
                    break;
                case 406:
                    logger.error("A condition was violated (e.g., can't mix transactional and confirm mode)." +
                    " This can happen when a message is published in confirm mode, but the channel is transactional.");
                    break;
                case 530:
                    logger.error("Operation is not allowed, often due to incorrect broker configuration" +
                    " (e.g., trying to publish to a non-existent exchange).");
                    break;
                case 541:
                    logger.error("RabbitMQ internal crash or unexpected bug." +
                    " This can happen when there is a bug in RabbitMQ or when the server crashes unexpectedly.");
                    break;
                default:
                    logger.error("RabbitMQ message returned: " + returnedMessage.getMessage() + 
                        ", replyCode: " + returnedMessage.getReplyCode() + ", replyText: " + returnedMessage.getReplyText() + 
                        ", exchange: " + returnedMessage.getExchange() + ", routingKey: " + returnedMessage.getRoutingKey());
            }
        });

        // This callback is invoked when a message is acknowledged by RabbitMQ
        // It provides information about the message and whether it was acknowledged or not
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {            
            if (ack) {
                logger.info("RabbitMQ message acknowledged: " + correlationData + ", ack: " + ack + ", cause: " + cause);
            } else {
                logger.warn("RabbitMQ message not acknowledged: " + correlationData + ", ack: " + ack + ", cause: " + cause);
            }
        });

        return rabbitTemplate;
    }
}
