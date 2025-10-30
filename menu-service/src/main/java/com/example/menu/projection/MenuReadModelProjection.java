package com.example.menu.projection;

import com.example.menu.events.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

@Service
public class MenuReadModelProjection {
    private final MongoTemplate mongoTemplate;

    public MenuReadModelProjection(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @RabbitListener(queues = "menu.created")
    public void handleMenuItemCreated(MenuItemCreatedEvent event) {
        MenuItemReadModel readModel = new MenuItemReadModel();
        readModel.setId(event.getId());
        readModel.setName(event.getName());
        readModel.setDescription(event.getDescription());
        readModel.setPrice(event.getPrice());
        readModel.setCreatedAt(event.getCreatedAt());
        readModel.setActive(true);
        
        mongoTemplate.save(readModel, "menu_items_read");
    }

    @RabbitListener(queues = "menu.updated")
    public void handleMenuItemUpdated(MenuItemUpdatedEvent event) {
        MenuItemReadModel readModel = mongoTemplate.findById(event.getId(), MenuItemReadModel.class, "menu_items_read");
        if (readModel != null) {
            readModel.setName(event.getName());
            readModel.setDescription(event.getDescription());
            readModel.setPrice(event.getPrice());
            readModel.setUpdatedAt(event.getUpdatedAt());
            mongoTemplate.save(readModel, "menu_items_read");
        }
    }

    @RabbitListener(queues = "menu.deleted")
    public void handleMenuItemDeleted(MenuItemDeletedEvent event) {
        MenuItemReadModel readModel = mongoTemplate.findById(event.getId(), MenuItemReadModel.class, "menu_items_read");
        if (readModel != null) {
            readModel.setActive(false);
            readModel.setDeletedAt(event.getDeletedAt());
            mongoTemplate.save(readModel, "menu_items_read");
        }
    }
}