package org.example.pspotl.biztalkapi.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final StringRedisTemplate redisTemplate;
    private static final String IDEMPOTENCY_KEY_PREFIX = "idempotency:";

    /**
     * Redis SETNX를 활용한 중복 요청 방지 멱등성 체크
     * @param messageId 고유 메시지 ID
     * @param ttl 만료 시간 (예: 5분)
     * @return true이면 최초 요청(성공), false이면 중복 요청(차단)
     */
    public boolean checkAndSaveIdempotency(String messageId, Duration ttl) {
        String key = IDEMPOTENCY_KEY_PREFIX + messageId;
        // 키가 존재하지 않을 때만 값을 설정하고 true 반환 (Atomic 연산)
        Boolean isNew = redisTemplate.opsForValue().setIfAbsent(key, "PROCESSING", ttl);
        return Boolean.TRUE.equals(isNew);
    }
}