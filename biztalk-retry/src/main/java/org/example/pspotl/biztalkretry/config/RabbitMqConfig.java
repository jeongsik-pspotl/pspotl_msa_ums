package org.example.pspotl.biztalkretry.config;

import ch.qos.logback.classic.pattern.MessageConverter;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String DLQ_NAME = "biztalk.queue.dlq";

    @Bean
    public Queue biztalkDlq() {
        // Durable=true : RabbitMQ 재부팅 시에도 큐 유지
        return QueueBuilder.durable(DLQ_NAME).build();
    }
}
