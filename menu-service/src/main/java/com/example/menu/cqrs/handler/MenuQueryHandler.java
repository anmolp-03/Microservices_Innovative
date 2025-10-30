package com.example.menu.cqrs.handler;

import com.example.menu.model.MenuItem;
import com.example.menu.cqrs.query.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class MenuQueryHandler {
    private final MongoTemplate mongoTemplate;

    public MenuQueryHandler(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public MenuItem handle(GetMenuItemQuery query) {
        return mongoTemplate.findById(query.getId(), MenuItem.class);
    }

    public List<MenuItem> handle(GetAllMenuItemsQuery query) {
        Query mongoQuery = new Query();
        mongoQuery.addCriteria(Criteria.where("active").is(true));
        return mongoTemplate.find(mongoQuery, MenuItem.class);
    }

    public List<MenuItem> handle(SearchMenuItemsQuery query) {
        Query mongoQuery = new Query();
        mongoQuery.addCriteria(
            new Criteria().orOperator(
                Criteria.where("name").regex(query.getSearchTerm(), "i"),
                Criteria.where("description").regex(query.getSearchTerm(), "i")
            ).andOperator(
                Criteria.where("active").is(true)
            )
        );
        return mongoTemplate.find(mongoQuery, MenuItem.class);
    }
}