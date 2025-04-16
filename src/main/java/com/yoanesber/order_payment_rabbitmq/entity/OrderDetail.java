package com.yoanesber.order_payment_rabbitmq.entity;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class OrderDetail {
    private Order order; // Many-to-One relation to Order
    private String productId;  // Unique Product ID (e.g., "PRD12345")
    private String productName;  // Name of the product
    private BigDecimal productPrice;  // Price per unit of product
    private Integer quantity;  // Number of items ordered
    private BigDecimal subtotal;  // `quantity * productPrice`
    private BigDecimal discountAmount;  // Discount applied per item
    private BigDecimal totalPrice;  // Final price after discount: `subtotal - discountAmount`
    private String productImageUrl;  // URL of the product image
    private String notes;  // Additional notes, e.g., special instructions
}
