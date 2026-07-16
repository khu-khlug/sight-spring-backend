# 그룹 활동 일정 수정

그룹 활동(`GROUP_ACTIVITY`) 일정의 정보를 수정합니다. 회원 인증이 필요하며, 본인이 작성한 그룹 활동 일정만 수정할 수 있습니다. 운영진(`MANAGER`)이라도 타인이 작성한 그룹 활동 일정은 수정할 수 없습니다. `category`는 변경할 수 없으며(카테고리 변경은 [`update-schedule-category.md`](update-schedule-category.md) 참조), 그룹 활동 일정은 경험치(`expoint`)를 가지지 않습니다. 일정 시작(`scheduledAt`)·종료(`endAt`) 이후에도 시점 제한 없이 수정할 수 있습니다.

> 대상 일정이 그룹 활동(`GROUP_ACTIVITY`)이 아니면 이 엔드포인트로 수정할 수 없습니다.

## API

```
PATCH /schedules/group-activity/{scheduleId}
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
200 OK
```

응답 본문 필드는 [`get-schedule.md`](get-schedule.md) 응답과 동일합니다 (그룹 활동은 `checkCode`가 없고 `expoint`는 0).

### 테스트 케이스

1. 정상 수정 → 200
2. 없는 일정 → 404
3. 대상 일정이 `GROUP_ACTIVITY`가 아니면 → 400 (전용 엔드포인트 사용)
4. `endAt < scheduledAt` → 400
5. 일정 시작·종료 이후에도 `scheduledAt`/`endAt` 수정 가능 → 200 (시점 제한 없음)
6. 일반 회원(`USER`)이 본인 작성 일정 수정 → 200
7. 일반 회원이 타인 작성 일정 수정 시도 → 403
8. 운영진(`MANAGER`)이 본인 작성 일정 수정 → 200
9. 운영진(`MANAGER`)이 타인 작성 일정 수정 시도 → 403
10. 인증되지 않은 요청 → 401