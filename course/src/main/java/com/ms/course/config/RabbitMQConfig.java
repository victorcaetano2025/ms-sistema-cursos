package com.ms.course.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "SGCO.exchange";
    public static final String ROUTING_LESSON_COMPLETED = "lesson.completed";

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(EXCHANGE_NAME);
    }

}