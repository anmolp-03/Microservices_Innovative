package com.example.menu.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    public static final String MENU_EXCHANGE = "menu.events";
    public static final String MENU_CREATED_QUEUE = "menu.created";
    public static final String MENU_UPDATED_QUEUE = "menu.updated";
    public static final String MENU_DELETED_QUEUE = "menu.deleted";

    @Bean
    public DirectExchange menuExchange() {
        return new DirectExchange(MENU_EXCHANGE);
    }

    @Bean
    public Queue menuCreatedQueue() {
        return QueueBuilder.durable(MENU_CREATED_QUEUE).build();
    }

    @Bean
    public Queue menuUpdatedQueue() {
        return QueueBuilder.durable(MENU_UPDATED_QUEUE).build();
    }

    @Bean
    public Queue menuDeletedQueue() {
        return QueueBuilder.durable(MENU_DELETED_QUEUE).build();
    }

    @Bean
    public Binding menuCreatedBinding(Queue menuCreatedQueue, DirectExchange menuExchange) {
        return BindingBuilder.bind(menuCreatedQueue)
                .to(menuExchange)
                .with("menu.created");
    }

    @Bean
    public Binding menuUpdatedBinding(Queue menuUpdatedQueue, DirectExchange menuExchange) {
        return BindingBuilder.bind(menuUpdatedQueue)
                .to(menuExchange)
                .with("menu.updated");
    }

    @Bean
    public Binding menuDeletedBinding(Queue menuDeletedQueue, DirectExchange menuExchange) {
        return BindingBuilder.bind(menuDeletedQueue)
                .to(menuExchange)
                .with("menu.deleted");
    }

    @Bean
    public MessageConverter messageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }
}