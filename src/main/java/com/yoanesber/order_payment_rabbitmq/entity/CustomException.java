package com.yoanesber.order_payment_rabbitmq.entity;

import java.time.Instant;
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
public class CustomException {
    private String message;
    private Instant createdAt = Instant.now();

    public CustomException(String message) {
        this.message = message;
    }
}
