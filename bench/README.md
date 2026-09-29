# 벤치마크 스크립트

`docs/decision/no-spring-async-and-yes-keepalive.md`에서 다룬 도어락 웹훅 성능 비교에 쓰인 스크립트다. 커밋마다 그 시점에 테스트한 코드 구성(sync/async, keep-alive 유무)이 함께 스냅샷으로 남아 있다.

## 구성

- `mock-discord-webhook.py`: 고정 지연시간을 주는 Discord 웹훅 모의 서버. 실제 Discord 대신 사용해 외부 API 응답 지연 조건을 통제한다.
- `bench-door-lock-alert.js`: k6 스크립트. `/internal/door-lock/alert-die`에 요청률(RPS)을 단계적으로 올려가며 부하를 건다.

## 실행 방법

```bash
# 1. 모의 웹훅 서버 실행
python3 bench/mock-discord-webhook.py
# MOCK_DELAY_MS(기본 300), MOCK_PORT(기본 9000) 환경변수로 조정 가능

# 2. 앱을 DOOR_LOCK_ALERT_DISCORD_WEBHOOK=http://localhost:9000 로 기동

# 3. k6로 부하 실행
k6 run bench/bench-door-lock-alert.js
# BASE_URL, API_KEY 환경변수로 조정 가능
```
