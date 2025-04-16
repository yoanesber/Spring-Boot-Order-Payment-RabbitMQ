package com.yoanesber.order_payment_rabbitmq.config;

import org.aopalliance.aop.Advice;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ListenerFactoryConfig is a configuration class that defines two RabbitMQ listener container factories.
 * These factories are used to create listener containers for processing messages from RabbitMQ queues.
 * The successQueueFactory is configured with retry logic for successful message processing,
 * while the failedQueueFactory is configured for failed message processing without retries.
 */

@Configuration
public class ListenerFactoryConfig {
    @Bean
    public SimpleRabbitListenerContainerFactory successQueueFactory(
            ConnectionFactory connectionFactory,
            @Qualifier("successQueueRetryAdvice") Advice retryAdvice) {

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setAdviceChain(retryAdvice);
        return factory;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory failedQueueFactory(
            ConnectionFactory connectionFactory,
            @Qualifier("failedQueueRetryAdvice") Advice retryAdvice) {

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setAdviceChain(retryAdvice);
        return factory;
    }
}
