package com.ms.certificate.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "SGCO.exchange";
    public static final String ROUTING_CERTIFICATE_ISSUED = "certificate.issued";
    public static final String QUEUE_COURSE_COMPLETED = "certificate.course-completed.queue";
    public static final String ROUTING_COURSE_COMPLETED = "course.completed";

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue courseCompletedQueue() {
        return new Queue(QUEUE_COURSE_COMPLETED, true);
    }

    @Bean
    public Binding bindingCourseCompleted(Queue courseCompletedQueue, DirectExchange exchange) {
        return BindingBuilder.bind(courseCompletedQueue).to(exchange).with(ROUTING_COURSE_COMPLETED);
    }
}