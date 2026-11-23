# AGENT_GUIDE.md

## 1. Project Overview
- **Project Name:** `biztalk-system`
- **Goal:** Prototype project to compare and validate **Kafka vs RabbitMQ** message queue behavior on a **UMS (Alimtalk, PUSH, SMS, LMS)** workload.
- **Runtime / Infra:** Java 17+, Spring Boot 3.2.x, Spring Kafka, Spring AMQP (comparison baseline), Spring Data Redis, Resilience4j, Docker Compose / Minikube (Kubernetes), Prometheus & Grafana.
- **Execution Constraint:** Daily 1-hour session on macOS + JetBrains IDE using Gemini API free tier. Minimize unnecessary file scans, redundant commands, and retry loops to stay within daily/minute rate limits (`429`).

## 2. Repository Structure (Target Multi-Module)
- **Root (`biztalk-system`):** Gradle multi-module root (`settings.gradle`, `build.gradle`).
- **`biztalk-core`:** Shared DTOs (`BizTalkMessage`, channel enums, payloads), common constants/utilities, and shared messaging configs.
- **`biztalk-api`:** REST API Gateway / Producer (`POST /api/v1/messages/send` and test endpoints), request validation, and message publishing.
- **`biztalk-engine`:** Message Consumer worker (Alimtalk, PUSH, SMS, LMS), Redis idempotency (`SETNX`/TTL), Resilience4j rate limiting/circuit breaker, and external sender invocation[cite: 1].
- **`biztalk-webhook`:** REST endpoint receiving asynchronous delivery status callbacks (success/failure reasons) from external vendors[cite: 1].
- **`biztalk-retry`:** Dedicated retry processor handling delayed re-publishing (backoff/retry topic or DLQ) and final failure isolation[cite: 1].
- **`biztalk-mockserver`:** Mock external vendor server simulating Alimtalk/APNs/FCM/SMS responses (200 OK, 429, 500, latency injection)[cite: 1].
- **`infra` / `k8s`:** Docker Compose, Minikube/Kubernetes YAML manifests, Prometheus & Grafana configs, and helper scripts[cite: 1].

## 3. Strict Working Rules for the Agent
1. **Scope Discipline:** Only create or modify files explicitly requested in the prompt. Do NOT refactor unrelated modules or files unless asked.
2. **Low-Token Exploration:** Check directory structure and only open files directly relevant to the target module. Avoid dumping huge logs or scanning entire repositories repeatedly.
3. **Command Budget:** Run build/test commands (`./gradlew ...`) ONLY when explicitly requested in the prompt (max 1–2 attempts per turn), and keep output focused on failures/errors.
4. **No Hallucinated Dependencies:** Use standard Spring Boot 3.2.x / Spring Kafka / Spring Data Redis / Resilience4j conventions compatible with Java 17+.
5. **Commit-Friendly Output:** Keep code clean, compile-ready, and modular so changes can be reviewed and committed within a 1-hour block.

## 4. Domain & Reliability Conventions
- **Message Core Fields:** `messageId`, `userId`, `channel` (`ALIMTALK`, `PUSH`, `SMS`, `LMS`), `receiverNum`, `content`, `retryCount`, `createdAt`.
- **Idempotency:** Prevent duplicate sending by `messageId` via Redis (`SETNX` + TTL) before external mock send[cite: 1].
- **Throttling & Failure Isolation:** Handle vendor TPS limits and errors cleanly without infinite broker retry loops (use Retry Topic / DLT / Resilience4j RateLimiter & CircuitBreaker)[cite: 1].