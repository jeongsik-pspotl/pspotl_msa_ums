package org.example.pspotl.biztalkengine.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient mockServerWebClient(WebClient.Builder webClientBuilder) {
        // 1. Connection Pool 상세 설정 (High TPS 최적화)
        ConnectionProvider connectionProvider = ConnectionProvider.builder("custom-connection-pool")
                .maxConnections(1000)                   // 최대 유지할 커넥션 수 (concurrency 500 고려해 1000 설정)
                .pendingAcquireMaxCount(2000)           // 커넥션 풀이 가득 찼을 때 대기할 최대 요청 수
                .pendingAcquireTimeout(Duration.ofSeconds(5)) // 커넥션 획득 대기 타임아웃
                .maxIdleTime(Duration.ofSeconds(30))    // 사용되지 않은 Idle 커넥션 유지 시간
                .maxLifeTime(Duration.ofMinutes(5))     // 커넥션 최대 생존 시간 (DNS/LB 변경 반영용)
                .evictInBackground(Duration.ofSeconds(30)) // 백그라운드 주기적 유효하지 않은 커넥션 정리
                .lifo()                                 // LIFO 방식: 최근 사용한 커넥션을 재사용하여 TCP Keep-Alive 유지 극대화
                .build();

        // 2. Netty HttpClient 및 Socket Option 설정
        HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000) // TCP Handshake Connect Timeout (3초)
                .option(ChannelOption.SO_KEEPALIVE, true)          // TCP Keep-Alive 활성화
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(3, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(3, TimeUnit.SECONDS)));

        // 3. WebClient 생성
        return webClientBuilder
                .baseUrl("http://biztalk-mockserver:8085") // Base URL 설정 (소스 코드 깔끔화)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}