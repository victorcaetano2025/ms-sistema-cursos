package com.ms.progress.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "SGCO.exchange";
    
    // Filas deste microserviço
    public static final String QUEUE_ENROLLMENT = "progress.enrollment-created.queue";
    public static final String QUEUE_LESSON = "progress.lesson-completed.queue";
    
    // Routing keys
    public static final String ROUTING_ENROLLMENT_CREATED = "enrollment.created";
    public static final String ROUTING_LESSON_COMPLETED = "lesson.completed";
    public static final String ROUTING_COURSE_COMPLETED = "course.completed";

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(EXCHANGE_NAME);
    }

    // --- Fila 1: Inscrições Criadas ---
    @Bean
    public Queue enrollmentCreatedQueue() {
        return new Queue(QUEUE_ENROLLMENT, true);
    }

    @Bean
    public Binding bindingEnrollment(Queue enrollmentCreatedQueue, DirectExchange exchange) {
        return BindingBuilder.bind(enrollmentCreatedQueue).to(exchange).with(ROUTING_ENROLLMENT_CREATED);
    }

    // --- Fila 2: Lições Concluídas ---
    @Bean
    public Queue lessonCompletedQueue() {
        return new Queue(QUEUE_LESSON, true);
    }

    @Bean
    public Binding bindingLesson(Queue lessonCompletedQueue, DirectExchange exchange) {
        return BindingBuilder.bind(lessonCompletedQueue).to(exchange).with(ROUTING_LESSON_COMPLETED);
    }
}