# AGENT_GUIDE.md

## 1. Project Overview
- **Goal:** Prototype project to compare and validate **Kafka vs RabbitMQ** message queue behavior on a **UMS (Alimtalk, PUSH, SMS, LMS)** workload.
- **Runtime / Infra:** Java 17+, Spring Boot, Spring Kafka, Spring AMQP (comparison target), Spring Data Redis, Resilience4j, Docker Compose / Minikube (Kubernetes), Prometheus & Grafana.
- **Execution Constraint:** Daily 1-hour session on macOS + JetBrains IDE using Gemini API free tier. Minimize unnecessary file scans, redundant commands, and retry loops to stay within daily/minute rate limits (`429`).

## 2. Repository Structure (Target Multi-Module)
- `app-common`: Shared DTOs (`BizTalkMessage`, channel enums, payloads), common constants/utilities.
- `app-producer-api`: REST API & web/test send endpoint (`/api/v1/test/...`), message publishing to Kafka (and comparison baseline).
- `app-consumer-worker`: Kafka consumers, Redis idempotency (`SETNX`/TTL), rate limiting/throttling, external mock sender call, Retry/DLT handling.
- `infra` / `k8s`: Docker Compose, Minikube/Kubernetes YAML manifests, Prometheus & Grafana configs, helper scripts.

## 3. Strict Working Rules for the Agent
1. **Scope Discipline:** Only create or modify files explicitly requested in the prompt. Do NOT refactor unrelated files or modules unless asked.
2. **Low-Token Exploration:** Check directory structure and only open files directly relevant to the task. Avoid dumping huge logs or scanning entire repositories repeatedly.
3. **Command Budget:** Run build/test commands (`./gradlew ...`) ONLY when explicitly requested in the prompt, and keep output focused on failures/errors.
4. **No Hallucinated Dependencies:** Use standard Spring Boot / Spring Kafka / Spring Data Redis / Resilience4j conventions compatible with Java 17+ and Spring Boot 3.x.
5. **Commit-Friendly Output:** Keep code clean, compile-ready, and modular so changes can be reviewed and committed within a 1-hour block.

## 4. Domain & Reliability Conventions
- **Message Core Fields:** `messageId`, `userId`, `channel` (`ALIMTALK`, `PUSH`, `SMS`, `LMS`), `receiverNum`, `content`, `retryCount`, `createdAt`.
- **Idempotency:** Prevent duplicate sending by `messageId` via Redis with TTL before external mock send.
- **Throttling & Failure Isolation:** Handle TPS limits and external vendor errors cleanly without infinite broker retry loops (use retry topic / DLT / circuit breaker / backoff patterns).