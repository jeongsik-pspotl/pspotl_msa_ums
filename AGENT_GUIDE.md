# AGENT_GUIDE.md

## 1. 프로젝트 개요 (Project Overview)
- **프로젝트명:** `biztalk-system`
- **목표:** **UMS (Alimtalk, PUSH, SMS, LMS)** 워크로드 환경에서 **Kafka vs RabbitMQ** 메시지 큐의 동작과 성능을 비교·검증하기 위한 프로토타입 프로젝트입니다.
- **런타임 및 기술 스택 (Runtime / Infra):** Java 17+, Spring Boot 3.2.x, Spring Kafka, Spring AMQP (비교 기준선), Spring Data Redis, Resilience4j, Docker Compose / Minikube (Kubernetes), Prometheus & Grafana
- **실행 제약 조건:** macOS + JetBrains IDE 환경에서 Gemini API 무료 티어를 활용해 하루 1시간 단위로 개발을 진행합니다. 일일/분당 요청 한도(`429`) 초과를 방지하기 위해 불필요한 파일 전체 스캔, 중복 명령어 실행, 무한 재시도 루프를 최소화해야 합니다.

## 2. 저장소 구조 및 패키지 기준 (Target Multi-Module & Package Structure)
- **기본 루트 패키지 (Base Package Standard):** 모든 Java 소스는 **`com.biztalk.*`** 하위 패키지를 기본 기준으로 사용합니다. 기존 `com.biztalk.*` 소스 또는 설정과 충돌하는 임의의 루트 패키지(예: `com.example.*`, `com.ums.*` 등)를 생성하거나 혼합하지 않습니다.
- **모듈별 패키지 매핑 (Module-to-Package Mapping):**
    - **Root (`biztalk-system`):** Gradle 멀티 모듈 루트 (`settings.gradle`, `build.gradle`)
    - **`biztalk-core` (`com.biztalk.core.*`):** 공통 DTO (`BizTalkMessage`, `ChannelType` 등), 공통 상수/유틸리티, 공통 메시징 설정 (`com.biztalk.core.domain`, `com.biztalk.core.config` 등)
    - **`biztalk-api` (`com.biztalk.api.*`):** REST API Gateway / Producer (`POST /api/v1/messages/send` 및 테스트 엔드포인트), 요청 검증 및 메시지 발행 (`com.biztalk.api.controller`, `com.biztalk.api.dto`, `com.biztalk.api.service` 등)
    - **`biztalk-engine` (`com.biztalk.engine.*`):** 메시지 Consumer 워커 (Alimtalk, PUSH, SMS, LMS), Redis 멱등성 제어 (`SETNX`/TTL), Resilience4j Rate Limiter / Circuit Breaker, 외부 발송 모듈 호출
    - **`biztalk-webhook` (`com.biztalk.webhook.*`):** 외부 벤더로부터 비동기 발송 결과 콜백(성공/실패 사유)을 수신하는 REST 엔드포인트
    - **`biztalk-retry` (`com.biztalk.retry.*`):** 지연 재발행(Backoff / Retry Topic 또는 DLQ) 및 최종 실패 격리를 전담하는 재시도 프로세서
    - **`biztalk-mockserver` (`com.biztalk.mockserver.*`):** 외부 발송 벤더(Alimtalk / APNs / FCM / SMS)의 응답(200 OK, 429, 500, 지연 주입)을 시뮬레이션하는 Mock 서버
    - **`infra` / `k8s`:** Docker Compose, Minikube/Kubernetes YAML 매니페스트, Prometheus & Grafana 설정 및 실행 스크립트

## 3. 에이전트 엄수 작업 규칙 (Strict Working Rules)
1. **패키지 및 소스 기준 준수 (Package & Source Alignment):** 코드 생성·수정 및 탐색 시 반드시 `com.biztalk.*` 패키지 체계를 따릅니다. 대상 모듈의 기존 `src/main/java/com/biztalk/...` 경로와 `package` 선언을 먼저 확인해 기존 클래스와 패키지/임포트가 충돌하지 않게 맞춥니다.
2. **작업 범위 준수 (Scope Discipline):** 프롬프트에서 명시적으로 요청한 파일만 생성하거나 수정합니다. 별도 요청이 없는 한 관련 없는 모듈이나 기존 파일을 임의로 리팩토링하지 않습니다.
3. **저토큰 탐색 (Low-Token Exploration):** 대상 모듈의 `src/main/java/com/biztalk` 하위 디렉토리와 직접 관련된 파일만 우선 확인합니다. 방대한 로그를 한꺼번에 출력하거나 전체 저장소를 반복 스캔하지 않습니다.
4. **명령어 실행 제한 (Command Budget):** 빌드 및 테스트 명령어(`./gradlew ...`)는 프롬프트에 명시된 경우에만 실행하며(턴당 최대 1~2회 제한), 출력은 실패 및 에러 원인 파악에 필요한 핵심 내용만 확인합니다.
5. **환각 의존성 금지 (No Hallucinated Dependencies):** Java 17+ 환경과 호환되는 표준 Spring Boot 3.2.x / Spring Kafka / Spring Data Redis / Resilience4j 규약 및 의존성만 사용합니다.
6. **커밋 친화적 산출물 (Commit-Friendly Output):** 하루 1시간 작업 블록 내에 검토하고 커밋할 수 있도록 코드를 간결하고 컴파일 가능한 모듈 단위로 유지합니다.

## 4. 도메인 및 안정성 규약 (Domain & Reliability Conventions)
- **메시지 핵심 필드:** `messageId`, `userId`, `channel` (`ALIMTALK`, `PUSH`, `SMS`, `LMS`), `receiverNum`, `content`, `retryCount`, `createdAt`
- **멱등성 보장 (Idempotency):** 외부 Mock 발송 호출 전에 Redis(`SETNX` + TTL)를 통해 `messageId` 기준 중복 발송을 차단합니다.
- **스로틀링 및 장애 격리 (Throttling & Failure Isolation):** 브로커의 무한 재시도 루프에 빠지지 않도록 Retry Topic / DLT / Resilience4j RateLimiter 및 CircuitBreaker를 활용해 벤더 TPS 제한과 오류를 안전하게 처리합니다.