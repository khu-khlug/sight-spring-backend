# 세미나 일정 생성

세미나(`SEMINAR`, 총회) 일정을 생성합니다. 운영진(`MANAGER`) 인증이 필요합니다. 이 엔드포인트로 생성되는 일정의 `category`는 항상 `SEMINAR`로 고정되며 요청 바디로 받지 않습니다. 일정 생성과 동시에 빅세미나(`big_seminar`) 레코드가 함께 생성되며, 이를 위해 `isSummerSeason`·`isSpeakAfter` 값을 추가로 받습니다. 빅세미나 레코드의 식별자는 백엔드가 생성합니다. 출첵 코드는 `generateCheckCode=true`인 경우에만 백엔드가 4자리 숫자 코드를 생성·저장합니다.

> 운영진용 일반 일정은 [`create-schedule.md`](create-schedule.md), 그룹 활동 일정은 [`create-group-activity-schedule.md`](create-group-activity-schedule.md)를 참조하세요.

## API

```
POST /schedules/big-seminar
```

### 쿼리 파라미터

(해당 없음)

### 요청 바디

| 이름                |  타입  | 설명                                                                                          |
| :------------------ | :----: | :-------------------------------------------------------------------------------------------- |
| `title`             | 문자열 | 일정 제목                                                                                     |
| `location`          | 문자열 | 장소 (nullable)                                                                               |
| `scheduledAt`       | 문자열 | 시작 일시 (ISO 8601)                                                                          |
| `endAt`             | 문자열 | 종료 일시 (ISO 8601)                                                                          |
| `expoint`           |  숫자  | (선택, 기본 0) 출석 시 부여될 ExPoint. 0 이상                                                  |
| `generateCheckCode` | 불리언 | (선택, 기본 `false`) `true`면 백엔드가 4자리 숫자 출첵 코드를 생성·저장. `false`/미전송이면 출첵 코드 없음 |
| `isSummerSeason`    | 불리언 | 빅세미나: 여름 시즌 여부                                                                       |
| `isSpeakAfter`      | 불리언 | 빅세미나: 나중에 말하기 여부                                                                   |

### 응답 코드 및 응답 바디

```
201 Created
```

| 이름             |  타입  | 설명                                 |
| :--------------- | :----: |:-----------------------------------|
| `id`             |  숫자  | 생성된 일정 ID                          |
| `title`          | 문자열 | 일정 제목                              |
| `category`       | 문자열 | 항상 `SEMINAR`                       |
| `location`       | 문자열 | 장소 (nullable)                      |
| `state`          | 문자열 | 일정 상태 (`public`/`trash`)           |
| `scheduledAt`    | 문자열 | 시작 일시 (ISO 8601)                   |
| `endAt`          | 문자열 | 종료 일시 (ISO 8601)                   |
| `expoint`        |  숫자  | 출석 시 부여될 ExPoint                   |
| `checkCode`      | 문자열 | 백엔드가 생성한 출첵 코드 (생성 안 했으면 nullable) |
| `author`         |  숫자  | 작성자 회원 ID                          |
| `createdAt`      | 문자열 | 생성 일시 (ISO 8601)                   |
| `isSummerSeason` | 불리언 | 빅세미나: 여름 시즌 여부                     |
| `isSpeakAfter`   | 불리언 | 빅세미나: 나중에 말하기 여부                   |

### 테스트 케이스

1. 정상 등록 → 201, 응답에 `id`/`createdAt`이 포함되고 `category`는 `SEMINAR`다
2. 일정 생성과 함께 빅세미나(`big_seminar`) 레코드가 생성된다 (`isSummerSeason`/`isSpeakAfter` 저장)
3. `isSummerSeason` 또는 `isSpeakAfter`가 누락되면 400을 반환한다
4. `generateCheckCode=true` 시 백엔드가 4자리 숫자 출첵 코드를 생성·저장하고 응답 `checkCode`에 반환한다
5. `endAt < scheduledAt`이면 400을 반환한다
6. `expoint`가 음수이면 400을 반환한다
7. 인증되지 않은 요청 → 401
8. 일반 회원(`USER`)이 생성 시도 → 403
