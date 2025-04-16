package com.yoanesber.order_payment_rabbitmq.config;

import org.aopalliance.aop.Advice;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.yoanesber.order_payment_rabbitmq.recovery.LoggingRejectAndDontRequeueRecoverer;

/*
 * RetryConfig is a configuration class that defines two retry advice beans for RabbitMQ message processing.
 * The successQueueRetryAdvice is configured with retry logic for successful message processing,
 * while the failedQueueRetryAdvice is configured for failed message processing without retries.
 * These beans are used in the RabbitMQ listener container factories to handle message processing retries.
 */ 

@Configuration
public class RetryConfig {

    private static final int MAX_ATTEMPTS = 3;
    private static final long INITIAL_INTERVAL = 5000L; // 5 second
    private static final double MULTIPLIER = 1.0; // No multiplier, fixed interval
    private static final long MAX_INTERVAL = 10000L; // 10 seconds

    @Bean
    public Advice successQueueRetryAdvice() {
        return RetryInterceptorBuilder.stateless()
            .maxAttempts(MAX_ATTEMPTS)
            .backOffOptions(INITIAL_INTERVAL, MULTIPLIER, MAX_INTERVAL)
            .recoverer(new LoggingRejectAndDontRequeueRecoverer())
            .build();
    }

    @Bean
    public Advice failedQueueRetryAdvice() {
        return RetryInterceptorBuilder.stateless()
            .maxAttempts(MAX_ATTEMPTS)
            .backOffOptions(INITIAL_INTERVAL, MULTIPLIER, MAX_INTERVAL)
            .recoverer(new LoggingRejectAndDontRequeueRecoverer())
            .build();
    }
}