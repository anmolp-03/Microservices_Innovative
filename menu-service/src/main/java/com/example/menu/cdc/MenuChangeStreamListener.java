package com.example.menu.cdc;

import com.example.menu.events.*;
import com.mongodb.client.ChangeStreamIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.FullDocument;
import org.bson.Document;
import org.bson.BsonDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class MenuChangeStreamListener {
    private static final Logger log = LoggerFactory.getLogger(MenuChangeStreamListener.class);
    
    private final MongoTemplate mongoTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final ExecutorService executorService;

    public MenuChangeStreamListener(MongoTemplate mongoTemplate, RabbitTemplate rabbitTemplate) {
        this.mongoTemplate = mongoTemplate;
        this.rabbitTemplate = rabbitTemplate;
        this.executorService = Executors.newSingleThreadExecutor();
        log.info("MenuChangeStreamListener instantiated");
    }

    @PostConstruct
    public void startListening() {
        log.info("Starting MenuChangeStreamListener...");
        
        try {
            MongoCollection<Document> collection = mongoTemplate.getCollection("menu_items");
            log.info("Got MongoDB collection: {}", collection.getNamespace());
            
            ChangeStreamIterable<Document> changeStream = collection.watch()
                .fullDocument(FullDocument.UPDATE_LOOKUP);
            
            log.info("Change stream created, submitting listener task...");

            executorService.submit(() -> {
                log.info("Change stream listener thread started");
                try {
                    changeStream.forEach(changeDoc -> {
                        log.info("Change detected: operation={}", changeDoc.getOperationType());
                        handleChange(changeDoc);
                    });
                } catch (Exception e) {
                    log.error("Error in change stream listener", e);
                }
            });
            
            log.info("MenuChangeStreamListener started successfully");
        } catch (Exception e) {
            log.error("Failed to start MenuChangeStreamListener", e);
        }
    }

    private void handleChange(ChangeStreamDocument<Document> changeDoc) {
        String operationType = changeDoc.getOperationType().getValue();
        log.info("Handling change: type={}", operationType);
        
        Document fullDocument = changeDoc.getFullDocument();

        switch (operationType) {
            case "insert":
                if (fullDocument != null) {
                    MenuItemCreatedEvent createdEvent = new MenuItemCreatedEvent();
                    createdEvent.setId(fullDocument.getObjectId("_id").toString());
                    createdEvent.setName(fullDocument.getString("name"));
                    createdEvent.setDescription(fullDocument.getString("description"));
                    
                    // Handle price which might be stored as Double or BigDecimal
                    Object priceObj = fullDocument.get("price");
                    if (priceObj instanceof Double) {
                        createdEvent.setPrice(BigDecimal.valueOf((Double) priceObj));
                    } else if (priceObj instanceof BigDecimal) {
                        createdEvent.setPrice((BigDecimal) priceObj);
                    }
                    
                    // Handle createdAt
                    Object createdAtObj = fullDocument.get("createdAt");
                    if (createdAtObj instanceof java.util.Date) {
                        createdEvent.setCreatedAt(LocalDateTime.ofInstant(
                            ((java.util.Date) createdAtObj).toInstant(), 
                            ZoneId.systemDefault()
                        ));
                    }
                    
                    log.info("Publishing menu.created event for item: {}", createdEvent.getName());
                    rabbitTemplate.convertAndSend("menu.events", "menu.created", createdEvent);
                    log.info("Published menu.created event successfully");
                }
                break;

            case "update":
                if (fullDocument != null) {
                    MenuItemUpdatedEvent updatedEvent = new MenuItemUpdatedEvent();
                    updatedEvent.setId(fullDocument.getObjectId("_id").toString());
                    updatedEvent.setName(fullDocument.getString("name"));
                    updatedEvent.setDescription(fullDocument.getString("description"));
                    
                    // Handle price
                    Object priceObj = fullDocument.get("price");
                    if (priceObj instanceof Double) {
                        updatedEvent.setPrice(BigDecimal.valueOf((Double) priceObj));
                    } else if (priceObj instanceof BigDecimal) {
                        updatedEvent.setPrice((BigDecimal) priceObj);
                    }
                    
                    // Handle updatedAt
                    Object updatedAtObj = fullDocument.get("updatedAt");
                    if (updatedAtObj instanceof java.util.Date) {
                        updatedEvent.setUpdatedAt(LocalDateTime.ofInstant(
                            ((java.util.Date) updatedAtObj).toInstant(), 
                            ZoneId.systemDefault()
                        ));
                    }
                    
                    log.info("Publishing menu.updated event for item: {}", updatedEvent.getName());
                    rabbitTemplate.convertAndSend("menu.events", "menu.updated", updatedEvent);
                    log.info("Published menu.updated event successfully");
                }
                break;

            case "delete":
                BsonDocument documentKey = changeDoc.getDocumentKey();
                if (documentKey != null) {
                    MenuItemDeletedEvent deletedEvent = new MenuItemDeletedEvent();
                    deletedEvent.setId(documentKey.getObjectId("_id").getValue().toString());
                    deletedEvent.setDeletedAt(LocalDateTime.now());
                    
                    log.info("Publishing menu.deleted event for item ID: {}", deletedEvent.getId());
                    rabbitTemplate.convertAndSend("menu.events", "menu.deleted", deletedEvent);
                    log.info("Published menu.deleted event successfully");
                }
                break;
        }
    }
}