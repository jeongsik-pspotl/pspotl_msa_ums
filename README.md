# biztalk-system

**Kafka vs RabbitMQ 메시지 큐 비교·검증용 UMS (Alimtalk, PUSH, SMS, LMS) 프로토타입**

---

## 1. 프로젝트 개요 (Project Overview)
- **목표:** 대용량 UMS(알림톡, PUSH, SMS, LMS) 워크로드 환경에서 **Kafka vs RabbitMQ**의 처리 성능, 멱등성 제어, 스로틀링 및 장애 격리 동작을 비교·검증하는 프로토타입 구축
- **개발 방식:** macOS + JetBrains IDE 환경에서 Gemini API 기반 AI 에이전트 자동화 활용 (주 5일, 일 1시간 단위 점진적 개발)
- **패키지 표준:** `com.biztalk.*` 단일 루트 패키지 체계 적용

### 기술 스택 (Tech Stack)
- **Language & Framework:** Java 17+, Spring Boot 3.2.x
- **Messaging & Cache:** Spring Kafka, Spring AMQP (비교 기준선), Spring Data Redis (`SETNX` / TTL 멱등성)
- **Resilience & Observability:** Resilience4j (`RateLimiter`, `CircuitBreaker`), Prometheus, Grafana
- **Infra:** Docker Compose, Minikube (Kubernetes)

---

## 2. 멀티 모듈 구조 (Multi-Module Architecture)

| 모듈명 | 기본 패키지 | 포트 | 주요 역할 |
| :--- | :--- | :---: | :--- |
| **`biztalk-core`** | `com.biztalk.core.*` | - | 공통 도메인 DTO (`BizTalkMessage`, `ChannelType`), 상수 및 공통 메시징 설정 |
| **`biztalk-api`** | `com.biztalk.api.*` | `8080` | REST API Gateway / Producer (`POST /api/v1/messages/send`), 요청 검증 및 메시지 발행 |
| **`biztalk-engine`** | `com.biztalk.engine.*` | `8081` | 메시지 Consumer 워커, Redis 멱등성 검증(`SETNX`/TTL), Resilience4j 제어, Mock 벤더 발송 |
| **`biztalk-webhook`** | `com.biztalk.webhook.*` | `8082` | 외부 벤더 비동기 발송 결과 콜백(성공/실패 사유) 수신 REST 엔드포인트 |
| **`biztalk-retry`** | `com.biztalk.retry.*` | `8083` | 지연 재발행(Backoff / Retry Topic 또는 DLQ) 및 최종 실패 격리 처리 |
| **`biztalk-mockserver`** | `com.biztalk.mockserver.*` | `8085` | 외부 발송 벤더(Alimtalk / APNs / FCM / SMS) 응답(`200 OK`, `429`, `500`, 지연 주입) 시뮬레이션 |

---

## 3. WBS 진행 현황 (Work Breakdown Structure)

> **기준일:** 2026년 11월 28일 (토)  
> **현재 상태:** **3일차 진행 중 일시 중단** (Gemini 3 Flash 무료 티어 API 비율 제한 도달 및 `com.biztalk.*` 잔여 패키지 정리 단계)

| 일차 | 구분 | 주요 작업 내용 | 상태 | 비고 / 산출물 |
| :--- | :--- | :--- | :---: | :--- |
| **1일차 (월)** | **환경 & 규칙 세팅** | • GitHub 저장소 초기화 및 Gradle 멀티 모듈 구조 정의<br>• macOS + JetBrains + Gemini API 에이전트 연동<br>• `AGENT_GUIDE.md` 작성 및 초기 커밋 | **완료** | `AGENT_GUIDE.md` 기준선 수립 |
| **2일차 (화)** | **모듈 골격 & 빌드 정비** | • 6개 모듈 기본 설정(`build.gradle`, `application.yml`, 메인 클래스) 구성<br>• `java-library` / `api()` 의존성 및 `biztalk-engine` 빌드 이슈 해결<br>• `.gitignore` 반영 및 2일차 기준점 커밋 | **완료** | 6개 모듈 컴파일 통과 및 커밋 완료 |
| **3일차 (수)** | **핵심 도메인 & MQ 구현** | • `org.example.pspotl.*` 등 잔여 비표준 패키지 정리 (`com.biztalk.*` 통일)<br>• `biztalk-mockserver` 채널별 발송 응답 시뮬레이션 구현<br>• `biztalk-core` / `biztalk-api` 메시지 발행 구현<br>• `biztalk-engine` Consumer, Redis 멱등성(`SETNX`/TTL), Resilience4j 적용 | **진행 중 (중단)** | API 한도(`RPM 6/5`, `RPD 23/20`) 초과로 대기 중 |
| **4일차 (목)** | **재시도 · 웹훅 · 비교** | • `biztalk-retry` 지연 재발행 / DLT 격리 파이프라인 구현<br>• `biztalk-webhook` 발송 결과 콜백 처리 구현<br>• Kafka vs RabbitMQ 동작 및 처리 특성 비교 정리 | **예정** | 3일차 커밋 이후 순차 진행 |
| **5일차 (금)** | **인프라 · 검증 · 마무리** | • Docker Compose 및 Minikube 매니페스트 정비<br>• Prometheus / Grafana 메트릭 연동 및 부하·장애 시나리오 점검<br>• 최종 문서화 및 코드 정리 | **예정** | 최종 검증 및 문서 완결 |

---

## 4. 중단 시점 스냅샷 & 재개 가이드

### 현재 중단 사유 및 확인 사항
1. **API 비율 제한 도달:** `gemini-3-flash-preview` 모델의 분당/일일 요청 한도(`5 RPM`, `250K TPM`, `20 RPD`) 초과로 일시 중단되었습니다. (일일 한도는 매일 오후 5시 KST 전후 초기화)
2. **패키지 표준 반영 완료:** 일부 모듈에 남아 있던 `org.example.pspotl.*` 계열 잔여 패키지와 충돌하지 않도록 `AGENT_GUIDE.md`에 **`com.biztalk.*` 단일 패키지 표준 및 레거시 패키지 정리 규칙**을 추가했습니다.

### 한도 초기화 후 작업 재개 순서
1. **[3일차 - 사전 정리]** 6개 모듈 `src/main/java` 내 `org.example.*` 등 비표준 패키지를 `com.biztalk.<모듈명>` 기준으로 통일
2. **[3일차 - 1단계]** `biztalk-mockserver` (`com.biztalk.mockserver.*`) 발송 시뮬레이션 컨트롤러/서비스 구현
3. **[3일차 - 2단계]** `biztalk-core` 공통 상수 및 `biztalk-api` (`POST /api/v1/messages/send`) 발행 로직 구현
4. **[3일차 - 3단계]** `biztalk-engine` Consumer, Redis `SETNX`/TTL 멱등성, Resilience4j(`RateLimiter`/`CircuitBreaker`) 구현
5. **[3일차 - 4단계]** `./gradlew compileJava` 최소 확인(최대 1~2회) 후 3일차 커밋으로 기준점 고정

---

## 5. AI 에이전트 운용 원칙 (`AGENT_GUIDE.md` 요약)
- **범위 제한:** 한 번에 1~2개 모듈 또는 1개 책임 단위로 분할 요청하여 `429`/`503` 오류 및 중복 탐색 방지
- **명령 Budget:** `./gradlew` 빌드 명령은 명시된 검증 턴에서만 최대 1~2회 실행
- **패키지 규약:** 모든 신규 및 수정 코드는 `com.biztalk.*` 하위 패키지만 사용