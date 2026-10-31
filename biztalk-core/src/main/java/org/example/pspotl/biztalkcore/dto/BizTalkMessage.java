package org.example.pspotl.biztalkcore.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class BizTalkMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private String messageId;      // UUID 또는 비즈니스 유니크 키 (Idempotency Key)
    private String userId;         // 수신자 ID

    // channel, channelType, channel_type 어떤 형태의 JSON Key가 들어와도 매핑
    @JsonProperty("channel")
    @JsonAlias({"channelType", "channel_type", "channel"})
    private ChannelType channel;

    private String receiverNum;    // 전화번호 또는 Device Token
    private String content;        // 메시지 본문

    @Builder.Default
    private int retryCount = 0;    // 재시도 횟수 기본값 0

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime createdAt;

    public enum ChannelType {
        ALIMTALK, PUSH, SMS, LMS, UNKNOWN;

        @JsonCreator
        public static ChannelType fromString(String key) {
            if (key == null || key.trim().isEmpty()) {
                return UNKNOWN;
            }
            try {
                return ChannelType.valueOf(key.toUpperCase());
            } catch (IllegalArgumentException e) {
                return UNKNOWN; // null 대신 UNKNOWN 처리로 NPE 방지
            }
        }
    }
}
