package com.example.common.events;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// Enum for Order Status
enum OrderStatus {
    PENDING, CONFIRMED, PREPARING, READY, DELIVERED, CANCELLED
}

@Data
@NoArgsConstructor
public class MenuItemCreatedEvent {
    private String id;
    private String name;
    private String description;
    private BigDecimal price;
    private LocalDateTime createdAt;
}

@Data
@NoArgsConstructor
public class MenuItemUpdatedEvent {
    private String id;
    private String name;
    private String description;
    private BigDecimal price;
    private LocalDateTime updatedAt;
}

@Data
@NoArgsConstructor
public class MenuItemDeletedEvent {
    private String id;
    private LocalDateTime deletedAt;
}

@Data
@NoArgsConstructor
public class OrderCreatedEvent {
    private String id;
    private String customerId;
    private List<OrderItemEvent> items;
    private OrderStatus status;
    private LocalDateTime createdAt;
}

@Data
@NoArgsConstructor
public class OrderItemEvent {
    private String menuItemId;
    private int quantity;
    private BigDecimal price;
}

@Data
@NoArgsConstructor
public class OrderStatusUpdatedEvent {
    private String id;
    private OrderStatus status;
    private LocalDateTime updatedAt;
}

@Data
@NoArgsConstructor
public class BillGeneratedEvent {
    private String id;
    private String orderId;
    private BigDecimal totalAmount;
    private BigDecimal tax;
    private BigDecimal finalAmount;
    private LocalDateTime generatedAt;
}

@Data
@NoArgsConstructor
public class ReviewSubmittedEvent {
    private String id;
    private String orderId;
    private int rating;
    private String comment;
    private LocalDateTime submittedAt;
}