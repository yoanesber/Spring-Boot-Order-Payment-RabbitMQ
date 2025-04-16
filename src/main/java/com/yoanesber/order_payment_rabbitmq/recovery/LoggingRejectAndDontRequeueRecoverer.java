package com.yoanesber.order_payment_rabbitmq.recovery;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;
import org.springframework.retry.RetryContext;
import org.springframework.retry.support.RetrySynchronizationManager;
import org.springframework.stereotype.Component;

/**
 * LoggingRejectAndDontRequeueRecoverer is a custom recoverer that extends RejectAndDontRequeueRecoverer.
 * It logs the error message and retry count when the maximum number of retries is reached.
 * This class is used to handle message recovery in RabbitMQ when retries are exhausted.
 */ 

 @Component
public class LoggingRejectAndDontRequeueRecoverer extends RejectAndDontRequeueRecoverer {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void recover(Message message, Throwable cause) {
        int retryCount = -1;

        // Extract RetryContext if possible (e.g., wrapped in ListenerExecutionFailedException)
        if (cause instanceof ListenerExecutionFailedException listenerEx) {
            Object context = listenerEx.getFailedMessage().getMessageProperties()
                    .getHeaders().get("x-death");

            // Optional: You could try to extract retry count from headers if custom header used
            if (context instanceof RetryContext retryContext) {
                retryCount = retryContext.getRetryCount();
            } 
        } 
        
        // If retryCount is still -1, attempt to extract it from RetrySynchronizationManager
        if (retryCount == -1) {
            // Attempt to extract retry count from RetrySynchronizationManager
            retryCount = Optional.ofNullable(RetrySynchronizationManager.getContext())
                    .map(RetryContext::getRetryCount)
                    .orElse(-1);
        }

        // If retryCount is still -1, it means we couldn't extract it
        if (retryCount == -1) {
            logger.error("Retry count could not be determined. Defaulting to -1.");
        }

        // Log the error message and retry count
        logger.error("Message recovery invoked after retries exhausted.");
        logger.error("Retry Count {}: Max retries reached.", retryCount);
        logger.error("Message: {}", new String(message.getBody()));
        logger.error("Cause: {}", cause.getMessage());

        // Call parent logic to reject and not requeue
        super.recover(message, cause);
    }
}