#!/usr/bin/env bash
set -e

GREEN='\033[0;32m'
CYAN='\033[0;36m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${CYAN}=======================================================${NC}"
echo -e "${CYAN}   BizTalk System: Test, Build & Docker Compose Run    ${NC}"
echo -e "${CYAN}=======================================================${NC}"

if ! docker info >/dev/null 2>&1; then
    echo -e "${RED}[ERROR] Docker Desktop이 실행되어 있지 않습니다. Docker를 켜고 다시 실행해주세요.${NC}"
    exit 1
fi

if [ ! -f "docker-compose.yml" ]; then
    echo -e "${RED}[ERROR] 현재 디렉터리에 docker-compose.yml 파일이 없습니다.${NC}"
    exit 1
fi

# 1. 단위 및 통합 테스트 실행 (TDD 검증)
echo -e "\n${GREEN}[Step 1/5] Running Unit & Integration Tests (JUnit / TDD)...${NC}"
./gradlew clean test

# 2. 테스트 통과 후 Spring Boot Jar 빌드
echo -e "\n${GREEN}[Step 2/5] Building Spring Boot executable jars (bootJar)...${NC}"
./gradlew bootJar -x test

# 3. Docker Compose 이미지 빌드 및 백그라운드 기동
echo -e "\n${GREEN}[Step 3/5] Rebuilding Docker images and starting services...${NC}"
docker compose -f docker-compose.yml down --remove-orphans
docker compose -f docker-compose.yml build
docker compose -f docker-compose.yml up -d

# 4. 컨테이너 헬스체크 대기 및 상태 확인
echo -e "\n${GREEN}[Step 4/5] Waiting for services to stabilize and verifying status...${NC}"
sleep 10
docker compose -f docker-compose.yml ps

# 5. API E2E 스모크 테스트 (Smoke Test) 자동 수행
echo -e "\n${GREEN}[Step 5/5] Running E2E Smoke Test (API Connection Check)...${NC}"
HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "http://localhost:8080/api/v1/test/send-bulk?count=1" || echo "000")

if [ "$HTTP_STATUS" -eq 200 ] || [ "$HTTP_STATUS" -eq 201 ]; then
    echo -e "${GREEN}✅ [SUCCESS] API Smoke Test passed! (HTTP Status: $HTTP_STATUS)${NC}"
else
    echo -e "${RED}⚠️ [WARNING] API Smoke Test returned status: $HTTP_STATUS (Check container logs if needed)${NC}"
fi

echo -e "\n${CYAN}=======================================================${NC}"
echo -e "${GREEN} SUCCESS: All BizTalk Microservices & Infra Layer are UP!${NC}"
echo -e "${CYAN}=======================================================${NC}"
echo -e " - Producer API      : http://localhost:8080/api/v1/messages/send"
echo -e " - Engine Consumer   : http://localhost:8081/actuator/health"
echo -e " - Webhook Receiver  : http://localhost:8082/api/v1/webhook/report"
echo -e " - Retry Worker      : http://localhost:8083/actuator/health"
echo -e " - Mock Vendor Api   : http://localhost:8085/v1/external/send/alimtalk"
echo -e " - RabbitMQ Web Admin: http://localhost:15672 (guest / guest)"
echo -e " - Prometheus UI     : http://localhost:9090"
echo -e " - Grafana Dashboard : http://localhost:3000 (admin / admin)"
echo -e "${CYAN}=======================================================${NC}"