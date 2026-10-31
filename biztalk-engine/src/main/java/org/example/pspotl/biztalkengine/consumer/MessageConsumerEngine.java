package org.example.pspotl.biztalkengine.consumer;

import com.rabbitmq.client.Channel;
import io.github.resilience4j.ratelimiter.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.pspotl.biztalkcore.config.CommonRabbitConfig;
import org.example.pspotl.biztalkcore.dto.BizTalkMessage;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.Duration;

@Slf4j
//@Component
@RequiredArgsConstructor
public class MessageConsumerEngine {

    private final RateLimiter consumerRateLimiter;
    private final RateLimiter alimtalkRateLimiter;
    private final ReactiveStringRedisTemplate reactiveRedisTemplate;
    private final WebClient mockServerWebClient;

    @RabbitListener(
            queues = CommonRabbitConfig.QUEUE_ALIMTALK,
            containerFactory = "rabbitListenerContainerFactory" // QoS 200이 적용된 Factory 지정
    )
    public void consumeAlimtalk(BizTalkMessage message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
        String lockKey = "idempotency:" + message.getMessageId();

        try {
            // 0. RateLimiter 권한 획득
            if (!consumerRateLimiter.acquirePermission()) {
                channel.basicNack(tag, false, true);
                return;
            }

            // 1. Reactive Pipeline 구성
            Mono<Boolean> processPipeline = reactiveRedisTemplate.opsForValue()
                    .setIfAbsent(lockKey, "PROCESSING", Duration.ofMinutes(5))
                    .flatMap(isFirst -> {
                        if (!Boolean.TRUE.equals(isFirst)) {
                            log.warn("[DUPLICATE DETECTED] MessageId: {}", message.getMessageId());
                            return Mono.just(false);
                        }

                        if (!alimtalkRateLimiter.acquirePermission()) {
                            return reactiveRedisTemplate.delete(lockKey)
                                    .then(Mono.error(new IllegalStateException("RATE_LIMIT_EXCEEDED")));
                        }

                        return mockServerWebClient.post()
                                .uri("/v1/external/send/alimtalk")
                                .bodyValue(message)
                                .retrieve()
                                .bodyToMono(String.class)
                                .timeout(Duration.ofSeconds(3))
                                .flatMap(res -> reactiveRedisTemplate.opsForValue()
                                        .set(lockKey, "SUCCESS", Duration.ofDays(1))
                                        .thenReturn(true));
                    })
                    // 예외 발생 시 Lock 키 자동 삭제
                    .onErrorResume(e -> reactiveRedisTemplate.delete(lockKey).then(Mono.error(e)));

            // 파이프라인 실행
            Boolean isSuccess = processPipeline.block();

            if (Boolean.TRUE.equals(isSuccess)) {
                channel.basicAck(tag, false);
            } else {
                channel.basicAck(tag, false); // 중복 건 ACK
            }

        } catch (Exception e) {
            log.error("[CONSUME ERROR] MessageId: {}, Error: {}", message.getMessageId(), e.getMessage());
            try {
                boolean requeue = e.getCause() instanceof IllegalStateException &&
                        "RATE_LIMIT_EXCEEDED".equals(e.getCause().getMessage());
                channel.basicNack(tag, false, requeue);
            } catch (IOException ex) {
                log.error("[NACK ERROR] MessageId: {}", message.getMessageId(), ex);
            }
        }
    }
}