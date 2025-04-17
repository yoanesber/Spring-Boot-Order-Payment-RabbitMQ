package com.yoanesber.order_payment_rabbitmq.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

/**
 * RetryTemplateConfig is a configuration class that sets up a RetryTemplate for retrying operations.
 * It defines the maximum number of retry attempts and the back-off period between retries.
 * This configuration is useful for handling transient errors in message processing.
 */

@Configuration
public class RetryTemplateConfig {

    private static final int MAX_ATTEMPTS = 3; // Maximum number of retry attempts
    private static final long BACK_OFF_PERIOD = 2000; // Back-off period in milliseconds (2 seconds)

    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate template = new RetryTemplate();

        // Retry 3 times
        SimpleRetryPolicy policy = new SimpleRetryPolicy();
        policy.setMaxAttempts(MAX_ATTEMPTS);
        template.setRetryPolicy(policy);

        // Fixed 2-second delay between retries
        FixedBackOffPolicy backOff = new FixedBackOffPolicy();
        backOff.setBackOffPeriod(BACK_OFF_PERIOD);
        template.setBackOffPolicy(backOff);

        return template;
    }
}