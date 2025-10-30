package com.example.menu.cqrs.query;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetAllMenuItemsQuery {
    private int page;
    private int size;
    private String sortBy;
}
