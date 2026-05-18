# 일정 수정

기존 일정의 정보를 수정합니다. 회원 인증이 필요하며, 일반 회원(`USER`)은 본인이 작성하고 `category=GROUP_ACTIVITY`인 일정만 수정할 수 있습니다. 또한 일반 회원은 일정의 `category`를 `GROUP_ACTIVITY` 외 값으로 변경할 수 없습니다. 그 외 모든 경우는 운영진(`MANAGER`)만 수정할 수 있습니다. 일정 시작(`scheduledAt`) 이후에도 수정이 가능합니다.

## API

```
PUT /schedules/{scheduleId}
```

### 쿼리 파라미터

(해당 없음)

### 요청 바디

| 이름        |  타입  | 설명                                                                                                    |
| :---------- | :----: | :------------------------------------------------------------------------------------------------------ |
| `title`     | 문자열 | 일정 제목                                                                                               |
| `category`  | 문자열 | `CLUB`, `ACADEMIC`, `EXTERNAL`, `MANAGEMENT`, `GROUP_ACTIVITY`, `SEMINAR`, `AFTERPARTY`, `OTHER` 중 하나 |
| `location`  | 문자열 | 장소 (nullable)                                                                                         |
| `scheduledAt` | 문자열 | 시작 일시 (ISO 8601)                                                                                    |
| `endAt`   | 문자열 | 종료 일시 (ISO 8601)                                                                                    |
| `expoint`   |  숫자  | 0 이상                                                                                                  |

### 응답 코드 및 응답 바디

```
200 OK
```

응답 본문 필드는 [`get-schedule.md`](get-schedule.md) 응답과 동일합니다.

### 테스트 케이스

1. 정상 수정 → 200
2. 수정 시 `checkCode`는 변경되지 않는다
3. 없는 일정 → 404
4. `endAt < scheduledAt` → 400
5. 일정 시작 이후에도 `scheduledAt`/`endAt` 수정 가능 → 200
6. 일반 회원(`USER`)이 본인 작성 + `category=GROUP_ACTIVITY`인 일정 수정 → 200
7. 일반 회원이 타인 작성 그룹활동 일정 수정 시도 → 403
8. 일반 회원이 본인 작성이지만 `category != GROUP_ACTIVITY`인 일정 수정 시도 → 403
9. 일반 회원이 본인 작성 그룹활동 일정의 `category`를 다른 값으로 변경 시도 → 403
10. 인증되지 않은 요청 → 401