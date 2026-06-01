# 일정 카테고리 변경

기존 일정의 `category`를 변경합니다. 운영진(`MANAGER`) 인증이 필요합니다. 카테고리 변경은 빅세미나(`big_seminar`) 레코드의 생성/삭제를 동반하므로 일반 수정과 분리된 별도 엔드포인트입니다.

- **세미나(`SEMINAR`)로 변경:** 빅세미나 레코드를 새로 생성합니다. 이때 `isSummerSeason`·`isSpeakAfter`를 함께 받아야 합니다.
- **세미나(`SEMINAR`)에서 다른 카테고리로 변경:** 기존 빅세미나 레코드를 삭제합니다.
- 변경 대상 `category`로 `GROUP_ACTIVITY`는 허용하지 않습니다(그룹 활동은 일반 회원 영역).

> 일정의 다른 필드(제목/장소/시간/경험치) 수정은 각 카테고리의 수정 엔드포인트를 사용하세요.

## API

```
PATCH /schedules/{scheduleId}/category
```

### 쿼리 파라미터

(해당 없음)

### 요청 바디

| 이름             |  타입  | 설명                                                                                          |
| :--------------- | :----: | :-------------------------------------------------------------------------------------------- |
| `category`       | 문자열 | 변경할 카테고리. `CLUB`, `ACADEMIC`, `EXTERNAL`, `MANAGEMENT`, `AFTERPARTY`, `OTHER`, `SEMINAR` 중 하나 |
| `isSummerSeason` | 불리언 | (변경 대상이 `SEMINAR`일 때 필수) 빅세미나: 여름 시즌 여부                                     |
| `isSpeakAfter`   | 불리언 | (변경 대상이 `SEMINAR`일 때 필수) 빅세미나: 나중에 말하기 여부                                 |

### 응답 코드 및 응답 바디

```
200 OK
```

응답 본문 필드는 [`get-schedule.md`](get-schedule.md) 응답과 동일하며, 변경 후 카테고리가 `SEMINAR`인 경우 빅세미나 필드(`isSummerSeason`·`isSpeakAfter`)가 추가됩니다.

### 테스트 케이스

1. 운영진 카테고리 간 변경(예: `CLUB`→`ACADEMIC`) → 200
2. `SEMINAR`로 변경 시 빅세미나 레코드가 생성되고 `isSummerSeason`/`isSpeakAfter`가 저장된다 → 200
3. `SEMINAR`로 변경하는데 `isSummerSeason`/`isSpeakAfter`가 누락되면 → 400
4. `SEMINAR`에서 다른 카테고리로 변경 시 기존 빅세미나 레코드가 삭제된다 → 200
5. 변경 대상 `category`가 `GROUP_ACTIVITY`이면 → 400
6. 허용되지 않는 `category` 값이면 → 400
7. 없는 일정 → 404
8. 인증되지 않은 요청 → 401
9. 일반 회원(`USER`)이 변경 시도 → 403
