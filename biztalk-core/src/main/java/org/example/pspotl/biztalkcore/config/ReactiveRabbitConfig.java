package org.example.pspotl.biztalkcore.config;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.rabbitmq.Receiver;
import reactor.rabbitmq.ReceiverOptions;

@Configuration
public class ReactiveRabbitConfig {

    @Value("${spring.rabbitmq.host:localhost}")
    private String host;

    @Value("${spring.rabbitmq.port:5672}")
    private int port;

    @Value("${spring.rabbitmq.username:guest}")
    private String username;

    @Value("${spring.rabbitmq.password:guest}")
    private String password;

    @Bean
    public ConnectionFactory connectionFactory() {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(host);
        connectionFactory.setPort(port);
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        connectionFactory.useNio(); // Netty 기반 Non-blocking 소켓 I/O
        return connectionFactory;
    }

    @Bean
    public Receiver receiver(ConnectionFactory connectionFactory) {
        // Connection 생성을 비동기 Mono로 감싸고 백그라운드 스케줄러 할당
        Mono<Connection> connectionMono = Mono.fromCallable(() -> connectionFactory.newConnection("reactive-receiver"))
                .subscribeOn(Schedulers.boundedElastic())
                .cache(); // Connection 재활용

        // [수정] maxInMemoryFetchBatchSize 제거 후 connectionMono만 설정
        ReceiverOptions options = new ReceiverOptions()
                .connectionMono(connectionMono);

        return new Receiver(options);
    }
}