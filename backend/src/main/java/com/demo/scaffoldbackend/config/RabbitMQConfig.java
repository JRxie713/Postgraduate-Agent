package com.demo.scaffoldbackend.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "pg.agent.exchange";
    public static final String QUEUE_NAME = "pg.agent.queue";
    public static final String ROUTING_KEY = "pg.agent.routing";

    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange(EXCHANGE_NAME, true, false);
    }

    @Bean
    public Queue queue() {
        return new Queue(QUEUE_NAME, true);
    }

    @Bean
    public Binding binding(Queue queue, DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY);
    }
}
