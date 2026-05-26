# 출석 중인 스케줄 확인

현재 시각 기준으로 출석 가능한(출석 진행 중인) 일정 목록을 조회합니다. 회원 인증이 필요하며, 응답에는 출첵 코드(`checkCode`)가 포함되지 않습니다. 진행 중인 일정이 없으면 `schedule`은 `null`입니다.

## API

```
GET /active-schedules
```

### 쿼리 파라미터

(해당 없음)

### 요청 바디

(해당 없음)

### 응답 코드 및 응답 바디

```
200 OK
```

| 이름                    |   타입    | 설명                         |
| :---------------------- | :-------: | :--------------------------- |
| `count`                 |   숫자    | 반환된 항목 수               |
| `schedules`             | 객체 배열 | 출석 진행 중인 일정 목록     |
| `schedules.id`          |   숫자    | 일정 ID                      |
| `schedules.title`       |  문자열   | 일정 제목                    |
| `schedules.category`    |  문자열   | 카테고리 enum 값             |
| `schedules.location`    |  문자열   | 장소 (nullable)              |
| `schedules.state`       |  문자열   | 일정 상태 (`public`/`trash`) |
| `schedules.scheduledAt` |  문자열   | 시작 일시 (ISO 8601)         |
| `schedules.endAt`       |  문자열   | 종료 일시 (ISO 8601)         |
| `schedules.expoint`     |   숫자    | 출석 시 부여될 ExPoint       |
| `schedules.author`      |   숫자    | 작성자 회원 ID               |
| `schedule`              | 객체/null | 첫 번째 출석 진행 중 일정    |

### 판단 기준

- `scheduledAt ≤ now`
- `now ≤ endAt`
- `checkCode`가 `null`이 아닌 일정
- soft-delete된 일정은 제외

### 테스트 케이스

1. 출석 진행 중인 일정이 있으면 200, `schedule` 객체를 반환한다
2. 진행 중인 일정이 없으면 200, `schedule=null`을 반환한다
3. `endAt`이 지난 일정은 반환되지 않는다
4. `scheduledAt` 이전 일정은 반환되지 않는다
5. 어떤 경우에도 응답에 `checkCode` 필드가 포함되지 않는다
6. `checkCode`가 있고 `scheduledAt ≤ now ≤ endAt`인 일정만 반환한다
7. `checkCode`가 null인 일정은 시간 조건을 만족해도 제외된다
8. 이미 종료된 일정과 아직 시작하지 않은 일정은 모두 제외된다
