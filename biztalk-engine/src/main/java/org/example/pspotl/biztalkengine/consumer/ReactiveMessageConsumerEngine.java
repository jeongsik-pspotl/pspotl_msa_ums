package org.example.pspotl.biztalkengine.consumer;

import io.github.resilience4j.ratelimiter.RateLimiter;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.pspotl.biztalkcore.config.CommonRabbitConfig;
import org.example.pspotl.biztalkcore.dto.BizTalkMessage;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.ConsumeOptions;
import reactor.rabbitmq.Receiver;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReactiveMessageConsumerEngine {

    private final Receiver receiver;
    private final RateLimiter alimtalkRateLimiter;
    private final ReactiveStringRedisTemplate reactiveRedisTemplate;
    private final WebClient mockServerWebClient;

    @EventListener(ApplicationReadyEvent.class)
    public void startConsuming() {
        ConsumeOptions consumeOptions = new ConsumeOptions().qos(300); // Prefetch 300 지정

        receiver.consumeManualAck(CommonRabbitConfig.QUEUE_ALIMTALK, consumeOptions)
                // flatMapSequential 또는 flatMap으로 동시 처리 수(concurrency) 500~1000 제어
                .flatMap(acknowledgement -> {
                    BizTalkMessage message = parseMessage(acknowledgement.getBody()); // 메시지 역직렬화
                    String lockKey = "idempotency:" + message.getMessageId();

                    // 1. RateLimiter 비동기 체크 (동기 block 제거)
                    if (!alimtalkRateLimiter.acquirePermission()) {
                        acknowledgement.nack(true); // Requeue
                        return Mono.empty();
                    }

                    // 2. 완전 비동기 Reactive Pipeline (block() 절대 금지)
                    return reactiveRedisTemplate.opsForValue()
                            .setIfAbsent(lockKey, "PROCESSING", Duration.ofMinutes(5))
                            .flatMap(isFirst -> {
                                if (!Boolean.TRUE.equals(isFirst)) {
                                    log.warn("[DUPLICATE] MessageId: {}", message.getMessageId());
                                    acknowledgement.ack(); // 중복 메시지는 ACK 처리
                                    return Mono.empty();
                                }

                                // WebClient Config에 baseUrl이 설정되어 있으므로 상대 경로만 지정
                                return mockServerWebClient.post()
                                        .uri("/v1/external/send/alimtalk")
                                        .bodyValue(message)
                                        .retrieve()
                                        .bodyToMono(String.class)
                                        .timeout(Duration.ofSeconds(3))
                                        .flatMap(res -> reactiveRedisTemplate.opsForValue()
                                                .set(lockKey, "SUCCESS", Duration.ofDays(1))
                                                .then(Mono.fromRunnable(acknowledgement::ack)))
                                        .onErrorResume(e -> {
                                            log.error("[SEND ERROR] MessageId: {}", message.getMessageId(), e);
                                            return reactiveRedisTemplate.delete(lockKey)
                                                    .then(Mono.fromRunnable(() -> acknowledgement.nack(false)));
                                        });
                            });
                }, 500) // Concurrency: 스레드를 늘리는 게 아니라 비동기 요청을 동시에 500개 유지
                .subscribe();
    }

    private BizTalkMessage parseMessage(byte[] body) {
        // Jackson/Gson 등을 이용한 역직렬화 로직
        return new BizTalkMessage();
    }
}