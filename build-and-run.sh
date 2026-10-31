#!/usr/bin/env bash
set -e

GREEN='\033[0;32m'
CYAN='\033[0;36m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${CYAN}=======================================================${NC}"
echo -e "${CYAN}   BizTalk System: macOS Multi-Module Build & Run     ${NC}"
echo -e "${CYAN}=======================================================${NC}"

if ! docker info >/dev/null 2>&1; then
    echo -e "${RED}[ERROR] Docker Desktop이 실행되어 있지 않습니다. Docker를 켜고 다시 실행해주세요.${NC}"
    exit 1
fi

if [ ! -f "docker-compose.yml" ]; then
    echo -e "${RED}[ERROR] 현재 디렉터리에 docker-compose.yml 파일이 없습니다.${NC}"
    exit 1
fi

echo -e "\n${GREEN}[Step 1/4] Cleaning old build artifacts and building Spring Boot Jars...${NC}"
./gradlew clean bootJar -x test

echo -e "\n${GREEN}[Step 2/4] Rebuilding Docker images and starting services...${NC}"
docker compose -f docker-compose.yml down --remove-orphans
docker compose -f docker-compose.yml build
docker compose -f docker-compose.yml up -d

echo -e "\n${GREEN}[Step 3/4] Verifying container status...${NC}"
sleep 5
docker compose -f docker-compose.yml ps

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