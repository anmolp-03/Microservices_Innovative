package com.example.menu.events;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MenuItemDeletedEvent {
    private String id;
    private LocalDateTime deletedAt;
}
