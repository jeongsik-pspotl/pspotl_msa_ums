import http from 'k6/http';
import { check } from 'k6';
import { Trend, Counter } from 'k6/metrics';

// Custom Metrics 정의
const totalReqTrend = new Trend('custom_tps');            // 전체 TPS 트래킹용
const successReqCounter = new Counter('custom_success_reqs'); // 성공 요청 수 측정용

export const options = {
    scenarios: {
        tps_5000_test: {
            executor: 'ramping-arrival-rate',
            startRate: 100,               // 시작 초당 요청 수: 100 req/s
            timeUnit: '1s',

            // M1 Pro 자원 최적화
            preAllocatedVUs: 100,         // 초기 할당 VU
            maxVUs: 500,                 // 최대 VU 제한

            stages: [
                { duration: '2m', target: 50 },   // 2분 동안 50 req/s 로 부하 증가 (기존 파드 처리 한계 도달 유도)
                { duration: '3m', target: 200 },  // 3분 동안 200 req/s 로 부하 추가 증가 (오토스케일링 스케일 아웃 유도)
                { duration: '3m', target: 200 },  // 3분 동안 200 req/s 부하 유지 (증설된 파드로 응답 안정화되는지 확인)
                { duration: '2m', target: 10 },   // 2분 동안 10 req/s 로 부하 감소 (스케일 인 유도)
            ],
        },
    },
    thresholds: {
        http_req_failed: ['rate<0.05'],       // 에러율 5% 미만
        http_req_duration: ['p(95)<2000'],   // p(95) 2초 이내
    },
    // DNS 캐싱 및 TCP 커넥션 재사용 옵션 강제
    noConnectionReuse: false,
    discardResponseBodies: true, // 응답 바디 메모리 버림
};

export default function () {
    const url = 'http://localhost:8080/api/v1/messages/send';

    const payload = JSON.stringify({
        messageId: `msg-${__VU}-${__ITER}`,
        userId: `user-${__VU}`,
        channelType: 'ALIMTALK',
        content: '[비즈톡] 부하 테스트',
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
            'Connection': 'keep-alive',
        },
        timeout: '3s',
    };

    const res = http.post(url, payload, params);

    const isSuccess = check(res, {
        'is status 200': (r) => r.status === 200,
    });

    if (isSuccess) {
        successReqCounter.add(1);
    }
}

// 테스트 완료 후 콘솔 및 터미널 요약 결과 생성
export function handleSummary(data) {
    const totalDurationSeconds = data.state.testRunDurationMs / 1000;
    const totalRequests = data.metrics.http_reqs ? data.metrics.http_reqs.values.count : 0;
    const failedRequests = data.metrics.http_req_failed ? data.metrics.http_req_failed.values.passes : 0;
    const successRequests = totalRequests - failedRequests;

    const avgOverallTps = (totalRequests / totalDurationSeconds).toFixed(2);
    const avgSuccessTps = (successRequests / totalDurationSeconds).toFixed(2);

    const summaryText = `
===============================================================
                   k6 LOAD TEST TPS SUMMARY                    
===============================================================
- 총 테스트 수행 시간  : ${totalDurationSeconds.toFixed(2)} 초
- 총 시도한 요청 수    : ${totalRequests.toLocaleString()} 건
- 성공한 요청 수      : ${successRequests.toLocaleString()} 건
- 실패한 요청 수      : ${failedRequests.toLocaleString()} 건
---------------------------------------------------------------
* 전체 평균 TPS (Total)  : ${avgOverallTps} req/s
* 성공 평균 TPS (Success): ${avgSuccessTps} req/s
===============================================================
`;

    return {
        'stdout': summaryText, // 콘솔 터미널 출력
    };
}