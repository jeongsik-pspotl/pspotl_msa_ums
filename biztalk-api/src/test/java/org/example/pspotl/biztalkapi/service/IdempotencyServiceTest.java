package org.example.pspotl.biztalkapi.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private IdempotencyService idempotencyService;

    @Test
    @DisplayName("최초 요청 시 멱등성 검증 성공 (SETNX 성공)")
    void checkAndSaveIdempotency_Success() {
        // given
        String messageId = "msg-12345";
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).willReturn(true);

        // when
        boolean result = idempotencyService.checkAndSaveIdempotency(messageId, Duration.ofMinutes(5));

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("중복 요청 시 멱등성 검증 실패 (SETNX 실패)")
    void checkAndSaveIdempotency_Duplicate() {
        // given
        String messageId = "msg-12345";
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).willReturn(false);

        // when
        boolean result = idempotencyService.checkAndSaveIdempotency(messageId, Duration.ofMinutes(5));

        // then
        assertThat(result).isFalse();
    }
}
