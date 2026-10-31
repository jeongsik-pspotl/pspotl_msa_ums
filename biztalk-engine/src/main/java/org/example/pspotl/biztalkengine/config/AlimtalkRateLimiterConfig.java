package org.example.pspotl.biztalkengine.config;

// 1. Resilience4j 관련 import 명시
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig; // 👈 누락되었던 import 추가
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
// 2. 클래스 이름을 외부 라이브러리 클래스명과 다르게 변경 (충돌 방지)
public class AlimtalkRateLimiterConfig {

    @Bean
    public RateLimiter consumerRateLimiter() {
        // 이제 RateLimiterConfig는 Resilience4j의 클래스로 정상 인식됩니다.
        RateLimiterConfig config = RateLimiterConfig.custom()
                // 초당 허용 요청 수 (예: 초당 500건으로 제한)
                .limitForPeriod(500)
                // Refresh 주기: 1초마다 500개의 허용량 충전
                .limitRefreshPeriod(Duration.ofSeconds(1))
                // 권한 획득을 위해 최대 5초까지 대기
                .timeoutDuration(Duration.ofSeconds(5))
                .build();

        RateLimiterRegistry registry = RateLimiterRegistry.of(config);

        return registry.rateLimiter("alimtalkConsumerLimiter");
    }
}
