package com.example.menu.repo;

import com.example.menu.model.MenuItem;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface MenuRepository extends MongoRepository<MenuItem, String> {
    List<MenuItem> findByActiveTrue();
    List<MenuItem> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String name, String description);
}
