package org.example.pspotl.biztalkcore.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommonRabbitConfig {

    public static final String MAIN_EXCHANGE = "biztalk.direct.exchange";
    public static final String DLX_EXCHANGE = "biztalk.dlx.exchange";

    public static final String QUEUE_ALIMTALK = "biztalk.queue.alimtalk";
    public static final String QUEUE_PUSH = "biztalk.queue.push";
    public static final String QUEUE_SMS = "biztalk.queue.sms";
    public static final String QUEUE_DLQ = "biztalk.queue.dlq";

    public static final String ROUTING_ALIMTALK = "channel.alimtalk";
    public static final String ROUTING_PUSH = "channel.push";
    public static final String ROUTING_SMS = "channel.sms";
    public static final String ROUTING_DLQ = "channel.dlq";

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
        rabbitAdmin.setAutoStartup(true);
        return rabbitAdmin;
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        converter.setNullAsOptionalEmpty(true);
        return converter;
    }

    // [추가] 고성능 처리를 위한 RabbitListener Container Factory 튜닝
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setPrefetchCount(100); // 스레드당 미리 가져올 메시지 수 (5000 TPS 최적화)
        factory.setConcurrentConsumers(50);
        factory.setMaxConcurrentConsumers(100);
        return factory;
    }

    /* ================= Direct Exchange 선언 ================= */
    @Bean
    public DirectExchange mainExchange() {
        return new DirectExchange(MAIN_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange(DLX_EXCHANGE, true, false);
    }

    /* ================= Queue 선언 ================= */
    @Bean
    public Queue alimtalkQueue() {
        return QueueBuilder.durable(QUEUE_ALIMTALK)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", ROUTING_DLQ)
                .build();
    }

    @Bean
    public Queue pushQueue() {
        return QueueBuilder.durable(QUEUE_PUSH)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", ROUTING_DLQ)
                .build();
    }

    @Bean
    public Queue smsQueue() {
        return QueueBuilder.durable(QUEUE_SMS)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", ROUTING_DLQ)
                .build();
    }

    @Bean
    public Queue dlqQueue() {
        return QueueBuilder.durable(QUEUE_DLQ).build();
    }

    /* ================= Binding 선언 ================= */
    @Bean
    public Binding bindingAlimtalk() {
        return BindingBuilder.bind(alimtalkQueue()).to(mainExchange()).with(ROUTING_ALIMTALK);
    }

    @Bean
    public Binding bindingPush() {
        return BindingBuilder.bind(pushQueue()).to(mainExchange()).with(ROUTING_PUSH);
    }

    @Bean
    public Binding bindingSms() {
        return BindingBuilder.bind(smsQueue()).to(mainExchange()).with(ROUTING_SMS);
    }

    @Bean
    public Binding bindingDlq() {
        return BindingBuilder.bind(dlqQueue()).to(dlxExchange()).with(ROUTING_DLQ);
    }
}