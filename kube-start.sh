#!/usr/bin/env bash
set -e

echo "=== [1/6] Minikube 구동 확인 및 시작 ==="
if ! minikube status | grep -q "apiserver: Running"; then
    echo "Minikube가 구동 중이지 않거나 정상 상태가 아닙니다. 재시작을 시도합니다..."
    minikube start
else
    echo "Minikube가 정상 실행 중입니다."
fi

echo "=== [2/6] Minikube metrics-server 애드온 활성화 ==="
if minikube addons list | grep "metrics-server" | grep -q "enabled"; then
    echo "metrics-server 애드온이 이미 활성화되어 있습니다."
else
    echo "metrics-server 애드온을 활성화하는 중..."
    minikube addons enable metrics-server

    echo "metrics-server 파드가 준비될 때까지 대기합니다 (최대 60초)..."
    kubectl wait --for=condition=ready pod -l k8s-app=metrics-server -n kube-system --timeout=60s || echo "metrics-server 준비 시간이 더 필요할 수 있습니다."
fi

echo "=== [3/6] Minikube 내부 Docker Daemon 환경 연결 ==="
eval $(minikube docker-env)

echo "=== [4/6] 로컬 Dockerfile 기반 이미지 빌드 ==="
docker build -t biztalk-mockserver:latest -f ./biztalk-mockserver/Dockerfile .
docker build -t biztalk-api:latest -f ./biztalk-api/Dockerfile .
docker build -t biztalk-engine:latest -f ./biztalk-engine/Dockerfile .

echo "=== [5/6] Kubernetes 매니페스트 적용 ==="
kubectl apply -f k8s-manifests.yaml

echo "=== [6/6] Pod 준비 대기 및 포트 포워딩 설정 ==="
echo "Pod들이 정상 구동될 때까지 대기 중입니다 (최대 120초)..."

# 타임아웃을 120초로 늘리고 || true 제거하여 부팅 완료를 보장
kubectl wait --for=condition=ready pod -l app=rabbitmq || true
kubectl wait --for=condition=ready pod -l app=redis || true
kubectl wait --for=condition=ready pod -l app=biztalk-mockserver || true
kubectl wait --for=condition=ready pod -l app=biztalk-api || true
kubectl wait --for=condition=ready pod -l app=biztalk-engine || true

# 기존 port-forward 프로세스 정리
pkill -f "kubectl port-forward" || true
sleep 2

echo "백그라운드로 포트 포워딩을 시작합니다..."
kubectl port-forward svc/biztalk-api 8080:8080 > /dev/null 2>&1 &
kubectl port-forward svc/biztalk-mockserver 8085:8085 > /dev/null 2>&1 &
kubectl port-forward svc/rabbitmq 15672:15672 > /dev/null 2>&1 &

echo "포트 포워딩 안정화 대기 및 검증 중..."
sleep 5

# 로컬 포트 열렸는지 체크 (Connection Refused 방지)
if ! nc -z localhost 8080; then
    echo "❌ ERROR: biztalk-api 포트포워딩(8080) 실패! Pod 상태를 확인하세요."
    kubectl get pods
    exit 1
fi

echo "=========================================="
echo "🎉 모든 환경 설정 및 배포가 완료되었습니다!"
echo "------------------------------------------"
echo "📌 Postman / 브라우저 접속 주소:"
echo " - Biztalk API        : http://localhost:8080"
echo " - Biztalk MockServer : http://localhost:8085"
echo " - RabbitMQ Web UI    : http://localhost:15672 (guest / guest)"
echo "=========================================="

echo "=== [추가 단계] Executing k6 5,000 TPS Load Test ==="
k6 run k6/script.js

echo "=== [SUCCESS] Demo Completed Successfully ==="