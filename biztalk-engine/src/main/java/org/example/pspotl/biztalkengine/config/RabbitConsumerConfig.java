package org.example.pspotl.biztalkengine.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitConsumerConfig {

    // 1. Exchange 정의 (Producer와 동일)
    public static final String MAIN_EXCHANGE = "biztalk.direct.exchange";
    public static final String DLX_EXCHANGE = "biztalk.dlx.exchange";

    // 2. Queue 정의 (Producer와 동일)
    public static final String QUEUE_ALIMTALK = "biztalk.queue.alimtalk";
    public static final String QUEUE_PUSH = "biztalk.queue.push";
    public static final String QUEUE_SMS = "biztalk.queue.sms";
    public static final String QUEUE_DLQ = "biztalk.queue.dlq";

    // 3. Routing Key 정의 (Producer와 동일)
    public static final String ROUTING_ALIMTALK = "channel.alimtalk";
    public static final String ROUTING_PUSH = "channel.push";
    public static final String ROUTING_SMS = "channel.sms";
    public static final String ROUTING_DLQ = "channel.dlq";


    // 5,000 TPS 대응을 위한 동시성 Listener Container Factory 설정
//    @Bean
//    public MessageConverter jsonMessageConverter() {
//        return new Jackson2JsonMessageConverter();
//    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter) {

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL); // 유실 방지 Manual ACK
        factory.setConcurrentConsumers(50);
        factory.setMaxConcurrentConsumers(100);
        factory.setPrefetchCount(100); // 250에서 조금 줄여 워커 스레드 간 균등 분배
        return factory;
    }


    // --- Exchange 선언 ---
//    @Bean
//    public DirectExchange mainExchange() {
//        return new DirectExchange(MAIN_EXCHANGE, true, false);
//    }
//
//    @Bean
//    public DirectExchange dlxExchange() {
//        return new DirectExchange(DLX_EXCHANGE, true, false);
//    }

    // --- Queue 선언 (Dead Letter 옵션 동일 적용) ---
//    @Bean
//    public Queue alimtalkQueue() {
//        Map<String, Object> args = new HashMap<>();
//        args.put("x-dead-letter-exchange", DLX_EXCHANGE);
//        args.put("x-dead-letter-routing-key", ROUTING_DLQ);
//        return new Queue(QUEUE_ALIMTALK, true, false, false, args);
//    }
//
//    @Bean
//    public Queue pushQueue() {
//        return QueueBuilder.durable(QUEUE_PUSH)
//                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
//                .withArgument("x-dead-letter-routing-key", ROUTING_DLQ)
//                .build();
//    }
//
//    @Bean
//    public Queue smsQueue() {
//        return QueueBuilder.durable(QUEUE_SMS)
//                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
//                .withArgument("x-dead-letter-routing-key", ROUTING_DLQ)
//                .build();
//    }
//
//    @Bean
//    public Queue dlqQueue() {
//        return new Queue(QUEUE_DLQ, true, false, false);
//    }
//
//    // --- Binding 선언 ---
//    @Bean
//    public Binding bindingAlimtalk() {
//        return BindingBuilder.bind(alimtalkQueue()).to(mainExchange()).with(ROUTING_ALIMTALK);
//    }
//
//    @Bean
//    public Binding bindingPush() {
//        return BindingBuilder.bind(pushQueue()).to(mainExchange()).with(ROUTING_PUSH);
//    }
//
//    @Bean
//    public Binding bindingSms() {
//        return BindingBuilder.bind(smsQueue()).to(mainExchange()).with(ROUTING_SMS);
//    }
//
//    @Bean
//    public Binding bindingDlq() {
//        return BindingBuilder.bind(dlqQueue()).to(dlxExchange()).with(ROUTING_DLQ);
//    }
}
