package org.example.pspotl.biztalkapi.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.pspotl.biztalkcore.config.CommonRabbitConfig;
import org.example.pspotl.biztalkcore.dto.BizTalkMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
public class MessageController {

    private final RabbitTemplate rabbitTemplate;

    // Enum 채널 타입별 Routing Key 매핑
    private static final Map<BizTalkMessage.ChannelType, String> ROUTING_KEY_MAP = Map.of(
            BizTalkMessage.ChannelType.ALIMTALK, CommonRabbitConfig.ROUTING_ALIMTALK,
            BizTalkMessage.ChannelType.PUSH, CommonRabbitConfig.ROUTING_PUSH,
            BizTalkMessage.ChannelType.SMS, CommonRabbitConfig.ROUTING_SMS,
            BizTalkMessage.ChannelType.LMS, CommonRabbitConfig.ROUTING_SMS
    );

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @PostMapping("/send")
    public ResponseEntity<String> sendMessage(@RequestBody BizTalkMessage message) {

        // 1. Channel 파라미터 검증 (Enum null 체크)
        if (message.getChannel() == null) {
            return ResponseEntity.badRequest().body("channel 필드는 필수 입력값입니다. (ALIMTALK, PUSH, SMS, LMS)");
        }

        String routingKey = ROUTING_KEY_MAP.get(message.getChannel());

        if (routingKey == null) {
            return ResponseEntity.badRequest().body("지원하지 않는 채널 타입입니다: " + message.getChannel());
        }

        // 2. Message 기본 메타데이터 보완
        if (message.getMessageId() == null || message.getMessageId().trim().isEmpty()) {
            message.setMessageId(UUID.randomUUID().toString());
        }
        if (message.getCreatedAt() == null) {
            message.setCreatedAt(LocalDateTime.now());
        }

        // 3. DTO -> Map<String, Object> 메시지 페이로드 전환
//        Map<String, Object> messageMap = new HashMap<>();
//        messageMap.put("messageId", message.getMessageId());
//        messageMap.put("userId", message.getUserId());
//        messageMap.put("channel", message.getChannel().name());
//        messageMap.put("receiverNum", message.getReceiverNum());
//        messageMap.put("content", message.getContent());
//        messageMap.put("retryCount", message.getRetryCount());
//        messageMap.put("createdAt", message.getCreatedAt().format(DATE_FORMATTER));

        // 4. Map 구조를 JSON으로 직렬화하여 비동기 메시지 발행
        rabbitTemplate.convertAndSend(
                CommonRabbitConfig.MAIN_EXCHANGE,
                routingKey,
                message
        );

        log.info("[Producer] Message sent in Map payload. ID: {}, Channel: {}, RoutingKey: {}",
                message.getMessageId(), message.getChannel(), routingKey);

        return ResponseEntity.ok("ACCEPTED: " + message.getMessageId());
    }
}