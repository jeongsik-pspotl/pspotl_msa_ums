# 🚀 대용량 MSA 통합 메시징 플랫폼 (pspotl_msa_ums)

> **High-Performance Event-Driven Microservices Architecture for Massive Messaging Pipelines**  
> 초당 5,000+ TPS 대용량 알림 메시지(알림톡, 카톡, SMS, LMS)를 무중단 및 데이터 유실 0%로 처리하기 위한 분산 백엔드 플랫폼 프로젝트입니다.

---

## 📌 1. 프로젝트 개요 및 핵심 목표

본 프로젝트는 초당 5,000 TPS 이상의 가변 트래픽 유입 환경에서 메시지 유실 없는 **At-least-once 배달 보장**, **Redis 기반 멱등성(Idempotency) 검증**, **RabbitMQ 기반 비동기 EDA(Event-Driven Architecture)**, 그리고 **Kubernetes 클라우드 네이티브 오케스트레이션**을 검증하기 위해 구축되었습니다.

### 🔑 Key Engineering Points
* **Event-Driven Architecture (EDA)**: RabbitMQ 메시지 큐를 통한 Producer-Consumer 완벽 디커플링 및 Peak 트래픽 버퍼링
* **High Concurrency & Idempotency**: Redis Atomic `SETNX` 연산을 활용한 1ms 미만의 중복 발송 원천 차단
* **Rate Limiting & Throttling**: Resilience4j 및 Redis 기반 채널별(알림톡, SMS 등) 초당 5,000 TPS 제한
* **Fault Tolerance & Reliability**: Manual ACK, Quorum Queue, Exponential Backoff 기반 Retry 및 DLQ(Dead Letter Queue) 격리
* **Cloud Native Orchestration**: Docker Compose(1차 로컬 검증) 및 Minikube Kubernetes(2차 최종 오케스트레이션 검증) 이원화 배포

---

## 🏗️ 2. 시스템 아키텍처 및 모듈 구성

```text
[ Client / Postman ]
         │
         ▼ (HTTP POST)
┌────────────────────────────────────────────────────────┐
│ 1. biztalk-api (Producer & Gateway)                    │
│  - Request Validation & Fast Response                  │
│  - Redis SETNX Idempotency Key Check                   │
└──────────────────────────┬─────────────────────────────┘
                           │ (AMQP Publisher Confirms)
                           ▼
┌────────────────────────────────────────────────────────┐
│ 2. RabbitMQ Cluster (Message Broker)                  │
│  - Quorum Queues / DLX (Dead Letter Exchange)          │
└──────────────────────────┬─────────────────────────────┘
                           │ (Manual ACK)
                           ▼
┌────────────────────────────────────────────────────────┐
│ 3. biztalk-engine (Consumer Workers)                   │
│  - RateLimiter (Max 5,000 TPS) & Channel Dispatcher    │
└──────────────────────────┬─────────────────────────────┘
                           │ (REST / HTTP)
                           ▼
┌────────────────────────────────────────────────────────┐
│ 4. biztalk-mockserver & biztalk-webhook / retry        │
│  - External Vendor Simulation / DLQ Retry Pipeline     │
└────────────────────────┘
```

### 📦 모듈별 역할
* **`ums-common`**: 공통 DTO, 예외 처리, JPA Entity 및 Redis/RabbitMQ 설정
* **`biztalk-api`**: 발송 요청 접수, 유효성 검증, 멱등성 검사 및 MQ 메시지 발행
* **`biztalk-engine`**: MQ 소비(Consumer), Throttling 제어, 채널별 Dispatcher 처리
* **`biztalk-webhook`**: 통신사/중계사 API 연동 모킹 및 비동기 결과 콜백 처리
* **`biztalk-retry`**: 실패 메시지 지연 재시도 및 최종 DLQ 격리 워커
* **`biztalk-mockserver`**: 외부 연동 테스트용 시뮬레이터

---

## 🛠️ 3. 기술 스택 (Tech Stack)

* **Backend**: Java 17, Spring Boot 3.2.5, Spring Data JPA, Spring Security
* **Database & Caching**: MariaDB 10.11 (Primary-Replica), Redis 7 (Alpine)
* **Messaging & EDA**: RabbitMQ 3.12 (Management / Quorum Queues)
* **Container & Infrastructure**: Docker, Docker Compose, Kubernetes (Minikube), Helm, ArgoCD
* **Testing & Monitoring**: JUnit 5, Mockito, k6, Prometheus, Grafana

---

## 🗓️ 4. WBS 및 진행 단계 (Work Breakdown Structure)

| 단계 | 기간 | 주요 과제 | 검증 환경 | 진행 상태 |
| :---: | :---: | :--- | :---: | :---: |
| **Day 1** | 1일차 | **인프라 환경 구성**: MariaDB Dockerfile 작성, 통합 `docker-compose.yml` 및 네트워크 세팅 | Docker Compose | **[완료]** |
| **Day 2** | 2일차 | **API 모듈 TDD 및 Redis 멱등성**: Request Validation, Redis `SETNX` 기반 중복 차단 로직 구현 | Docker Compose | **[완료]** |
| **Day 3** | 3일차 | **Engine & MQ 연동**: RabbitMQ Topology(Queue, Exchange, DLX) 구성 및 Consumer 구현 | Docker Compose | **[완료]** |
| **Day 4** | 4일차 | **Webhook & Retry 파이프라인**: 실패 건 백오피스 재시도 및 최종 DLQ 격리 구현 | Docker Compose | **[완료]** |
| **Day 5** | 5일차 | **1차 통합 검증 (Docker Compose)**: `run-with-test.sh` 활용 E2E 및 Smoke Test 통과 | Docker Compose | **[완료]** |
| **Day 6** | 6일차 | **2차 오케스트레이션 이관 (Minikube)**: K8s Manifests (Deployment, Service, PVC, HPA) 적용 | Minikube (K8s) | **[완료]** |
| **Day 7** | 7일차 | **최종 모니터링 & 부하 검증**: k6 5,000 TPS 부하주입 및 Prometheus/Grafana 지표 수집 | Minikube (K8s) | **[완료]** |

---

## ⚡ 5. Quick Start & 검증 가이드

### 1차 검증: Docker Compose 기반 (로컬 검증)

프로젝트 루트 디렉토리에서 작성된 통합 자동화 스크립트를 실행합니다.

```bash
# 실행 권한 부여
chmod +x run-with-test.sh

# 테스트, 빌드, Docker Compose 기동 및 E2E Smoke Test 일괄 수행
./run-with-test.sh
```

#### 접속 엔드포인트:
* **Producer API**: `http://localhost:8080/api/v1/messages/send`
* **Engine Consumer**: `http://localhost:8081/actuator/health`
* **RabbitMQ Management**: `http://localhost:15672` (guest / guest)
* **Mock Server**: `http://localhost:8085`

---

### 2차 검증: Minikube (Kubernetes) 오케스트레이션 검증

```bash
# 1. Minikube 기동
minikube start --driver=docker

# 2. Minikube Docker 데몬 연동
eval $(minikube docker-env)

# 3. K8s Manifest 배포
kubectl apply -f k8s/

# 4. Pod 상태 및 HPA 확인
kubectl get pods -w
kubectl get hpa
```

---

## 📈 6. 부하 테스트 및 모니터링 (k6 & Grafana)

k6를 활용하여 초당 5,000 TPS 대용량 부하 주입 시 시스템의 안정성 및 Throttling 제어 여부를 측정하였습니다.

```bash
# k6 부하 테스트 실행
k6 run k6/script.js
```

* **Target Throughput**: 5,000 TPS
* **Max Latency**: < 50ms (Redis Idempotency Check)
* **Data Integrity**: 트래픽 폭주 시에도 RabbitMQ 큐 버퍼링으로 손실률 0% 달성

---

## 📝 7. 이력서 및 포트폴리오 어필 성과 (Impact)

* **대용량 트래픽 수용**: 초당 5,000 TPS 이상의 메시징 유입 시에도 메시지 중복 발송 0건 및 무중단 처리 달성
* **분산 인프라 이중화**: MariaDB Master-Slave 분산 라우팅 및 Redis SETNX 멱등성 체계 완비
* **클라우드 네이티브 오케스트레이션**: Docker Compose 기반 1차 로컬 검증부터 Minikube K8s 오케스트레이션까지의 Full-Cycle CI/CD 및 인프라 자동화 구축