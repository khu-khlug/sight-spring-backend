# 세미나 일정 수정

세미나(`SEMINAR`) 일정의 정보를 수정합니다. 운영진(`MANAGER`) 인증이 필요합니다. 이 엔드포인트는 세미나 카테고리 일정만 수정하며, 일정 필드와 함께 빅세미나(`big_seminar`) 정보(`isSummerSeason`·`isSpeakAfter`)를 갱신합니다. `category`는 변경할 수 없습니다(카테고리 변경은 [`update-schedule-category.md`](update-schedule-category.md) 참조). 일정 시작(`scheduledAt`)·종료(`endAt`) 이후에도 시점 제한 없이 수정할 수 있습니다.

> 운영진용 일반 일정은 [`update-schedule.md`](update-schedule.md), 그룹 활동 일정은 [`update-group-activity-schedule.md`](update-group-activity-schedule.md)를 참조하세요.

## API

```
PATCH /schedules/big-seminar/{scheduleId}
```

### 쿼리 파라미터

(해당 없음)

### 요청 바디

| 이름             |  타입  | 설명                        |
| :--------------- | :----: | :-------------------------- |
| `title`          | 문자열 | 일정 제목                   |
| `location`       | 문자열 | 장소 (nullable)             |
| `scheduledAt`    | 문자열 | 시작 일시 (ISO 8601)        |
| `endAt`          | 문자열 | 종료 일시 (ISO 8601)        |
| `expoint`        |  숫자  | 0 이상                      |
| `isSummerSeason` | 불리언 | 빅세미나: 여름 시즌 여부     |
| `isSpeakAfter`   | 불리언 | 빅세미나: 나중에 말하기 여부 |

### 응답 코드 및 응답 바디

```
200 OK
```

응답 본문 필드는 [`get-schedule.md`](get-schedule.md) 응답과 동일하며, 빅세미나 필드(`isSummerSeason`·`isSpeakAfter`)가 추가됩니다.

### 테스트 케이스

1. 정상 수정 → 200
2. 수정 시 `checkCode`는 변경되지 않는다
3. `isSummerSeason`/`isSpeakAfter`가 빅세미나 레코드에 반영된다
4. 없는 일정 → 404
5. 대상 일정이 `SEMINAR`가 아니면 → 400 (전용 엔드포인트 사용)
6. `endAt < scheduledAt` → 400
7. 일정 시작·종료 이후에도 `scheduledAt`/`endAt` 수정 가능 → 200 (시점 제한 없음)
8. 인증되지 않은 요청 → 401
9. 일반 회원(`USER`)이 수정 시도 → 403
