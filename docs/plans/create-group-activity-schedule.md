# 그룹 활동 일정 생성

그룹 활동(`GROUP_ACTIVITY`) 일정을 생성합니다. 회원 인증이 필요하며, 일반 회원(`USER`)과 운영진(`MANAGER`) 모두 생성할 수 있습니다. 이 엔드포인트로 생성되는 일정의 `category`는 항상 `GROUP_ACTIVITY`로 고정되며 요청 바디로 받지 않습니다. 그룹 활동 일정은 경험치(`expoint`)와 출첵 코드를 가지지 않습니다.

> 운영진용 일정(`CLUB`/`ACADEMIC` 등)은 [`create-schedule.md`](create-schedule.md), 세미나 일정은 [`create-big-seminar-schedule.md`](create-big-seminar-schedule.md)를 참조하세요.

## API

```
POST /schedules/group-activity
```

### 쿼리 파라미터

(해당 없음)

### 요청 바디

| 이름          |  타입  | 설명                       |
| :------------ | :----: | :------------------------- |
| `title`       | 문자열 | 일정 제목                  |
| `location`    | 문자열 | 장소 (nullable)            |
| `scheduledAt` | 문자열 | 시작 일시 (ISO 8601)       |
| `endAt`       | 문자열 | 종료 일시 (ISO 8601)       |

### 응답 코드 및 응답 바디

```
201 Created
```

| 이름          |  타입  | 설명                              |
| :------------ | :----: | :-------------------------------- |
| `id`          |  숫자  | 생성된 일정 ID                    |
| `title`       | 문자열 | 일정 제목                         |
| `category`    | 문자열 | 항상 `GROUP_ACTIVITY`             |
| `location`    | 문자열 | 장소 (nullable)                   |
| `state`       | 문자열 | 일정 상태 (`public`/`trash`)      |
| `scheduledAt` | 문자열 | 시작 일시 (ISO 8601)              |
| `endAt`       | 문자열 | 종료 일시 (ISO 8601)              |
| `expoint`     |  숫자  | 항상 0                            |
| `author`      |  숫자  | 작성자 회원 ID                    |
| `createdAt`   | 문자열 | 생성 일시 (ISO 8601)              |

### 테스트 케이스

1. 정상 등록 → 201, 응답에 `id`/`createdAt`이 포함되고 `category`는 `GROUP_ACTIVITY`다
2. 응답의 `expoint`는 항상 0이다
3. `endAt < scheduledAt`이면 400을 반환한다
4. 일반 회원(`USER`)이 생성 → 201
5. 운영진(`MANAGER`)이 생성 → 201
6. 인증되지 않은 요청 → 401