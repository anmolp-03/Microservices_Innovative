package com.example.menu.controller;

import com.example.menu.model.MenuItem;
import com.example.menu.repo.MenuRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/menu")
public class MenuController {
    private final MenuRepository repo;

    public MenuController(MenuRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<MenuItem> all() {
        return repo.findAll();
    }

    @PostMapping
    public MenuItem create(@RequestBody MenuItem item) {
        return repo.save(item);
    }
}
