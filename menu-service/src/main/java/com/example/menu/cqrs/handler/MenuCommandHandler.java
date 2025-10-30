package com.example.menu.cqrs.handler;

import com.example.menu.model.MenuItem;
import com.example.menu.cqrs.command.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Component
public class MenuCommandHandler {
    private final MongoTemplate mongoTemplate;

    public MenuCommandHandler(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Transactional
    public MenuItem handle(CreateMenuItemCommand command) {
        MenuItem item = new MenuItem();
        item.setName(command.getName());
        item.setDescription(command.getDescription());
        item.setPrice(command.getPrice());
        return mongoTemplate.save(item);
    }

    @Transactional
    public MenuItem handle(UpdateMenuItemCommand command) {
        MenuItem item = mongoTemplate.findById(command.getId(), MenuItem.class);
        if (item == null) {
            throw new IllegalArgumentException("Menu item not found");
        }
        
        item.setName(command.getName());
        item.setDescription(command.getDescription());
        item.setPrice(command.getPrice());
        item.setUpdatedAt(LocalDateTime.now());
        
        return mongoTemplate.save(item);
    }

    @Transactional
    public void handle(DeleteMenuItemCommand command) {
        MenuItem item = mongoTemplate.findById(command.getId(), MenuItem.class);
        if (item == null) {
            throw new IllegalArgumentException("Menu item not found");
        }
        
        item.setActive(false);
        item.setUpdatedAt(LocalDateTime.now());
        mongoTemplate.save(item);
    }
}