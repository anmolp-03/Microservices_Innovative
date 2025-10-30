package com.example.menu.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EnableAspectJAutoProxy
public class CqrsConfig {
    // Additional CQRS-specific beans can be added here if needed
}