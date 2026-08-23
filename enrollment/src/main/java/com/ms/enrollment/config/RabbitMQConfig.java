package com.ms.enrollment.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "SGCO.exchange";
    public static final String ROUTING_ENROLLMENT_CREATED = "enrollment.created";
    public static final String QUEUE_CERTIFICATE = "enrollment.certificate-issued.queue";
    public static final String ROUTING_CERTIFICATE_ISSUED = "certificate.issued";

    @Bean
    public MessageConverter jsonMessageConverter() {
      
         return new JacksonJsonMessageConverter();
    }

    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue certificateIssuedQueue() {
        return new Queue(QUEUE_CERTIFICATE, true);
    }

    @Bean
    public Binding bindingCertificate(Queue certificateIssuedQueue, DirectExchange exchange) {
        return BindingBuilder.bind(certificateIssuedQueue).to(exchange).with(ROUTING_CERTIFICATE_ISSUED);
    }
}