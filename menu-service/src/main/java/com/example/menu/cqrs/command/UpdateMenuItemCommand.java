package com.example.menu.cqrs.command;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMenuItemCommand {
    private String id;
    private String name;
    private String description;
    private BigDecimal price;
}
