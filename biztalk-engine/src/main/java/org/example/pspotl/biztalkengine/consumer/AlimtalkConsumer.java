package org.example.pspotl.biztalkengine.consumer;

import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

@Slf4j
//@Component
public class AlimtalkConsumer {

    private final StringRedisTemplate redisTemplate;

    public AlimtalkConsumer(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }


    @RabbitListener(queues = "biztalk.queue.alimtalk", containerFactory = "rabbitListenerContainerFactory")
    public void handleAlimtalkMessage(@Payload Map<String, Object> payload,
                                      Channel channel,
                                      @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {

        String messageId = (String) payload.get("messageId");
        String redisKey = "idempotency:msg:" + messageId;

        try {

            log.info("[Consumer] Received Alimtalk Message: {}", payload);
            log.info(" - Message ID: {}", payload.get("messageId"));
            log.info(" - Content: {}", payload.get("content"));

            // Redis SETNX 기반 멱등성(Idempotency) 검증 (24시간 유효)
            Boolean isNewMessage = redisTemplate.opsForValue().setIfAbsent(redisKey, "PROCESSED", Duration.ofHours(24));

            if (Boolean.FALSE.equals(isNewMessage)) {
                log.warn("Duplicate message detected, skipping processing. messageId={}", messageId);
                channel.basicAck(deliveryTag, false);
                return;
            }

            // 외부 알림톡 연동 비즈니스 로직 수행 (Mocking)
//             executeSendAlimtalk(payload);

            // 처리 성공 시 Manual ACK
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("Failed to process alimtalk message. Routing to DLQ. messageId={}", messageId, e);
            // Redis 키 원복 (재처리 기회 부여 시)
            redisTemplate.delete(redisKey);
            // requeue = false 설정으로 Dead Letter Exchange(DLQ)로 자동 이동
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
