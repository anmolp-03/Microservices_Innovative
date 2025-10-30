package com.example.menu.cqrs.query;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchMenuItemsQuery {
    private String searchTerm;
    private int page;
    private int size;
}
