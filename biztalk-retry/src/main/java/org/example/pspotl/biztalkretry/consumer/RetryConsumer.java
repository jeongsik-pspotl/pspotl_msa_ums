package org.example.pspotl.biztalkretry.consumer;

import org.example.pspotl.biztalkcore.config.CommonRabbitConfig;
import org.example.pspotl.biztalkcore.dto.BizTalkMessage;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class RetryConsumer {

    private final RabbitTemplate rabbitTemplate;
    private static final int MAX_RETRY = 3;

    @RabbitListener(queues = CommonRabbitConfig.QUEUE_DLQ, containerFactory = "rabbitListenerContainerFactory")
    public void processRetry(BizTalkMessage message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        log.info("[DLQ RECEIVED] MessageId: {}, Current Retry: {}", message.getMessageId(), message.getRetryCount());

        if (message.getRetryCount() < MAX_RETRY) {
            message.setRetryCount(message.getRetryCount() + 1);

            // Re-queue back to Main Exchange with delay
            rabbitTemplate.convertAndSend(CommonRabbitConfig.MAIN_EXCHANGE, CommonRabbitConfig.ROUTING_ALIMTALK, message);
            log.info("[RETRY RE-QUEUED] MessageId: {}", message.getMessageId());
        } else {
            log.error("[MAX RETRY EXCEEDED - STORE TO DISCARD/DB] MessageId: {}", message.getMessageId());
        }

        channel.basicAck(tag, false);
    }
}
