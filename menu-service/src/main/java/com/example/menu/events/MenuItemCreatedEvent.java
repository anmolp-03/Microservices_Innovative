package com.example.menu.events;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MenuItemCreatedEvent {
    private String id;
    private String name;
    private String description;
    private BigDecimal price;
    private LocalDateTime createdAt;
}
