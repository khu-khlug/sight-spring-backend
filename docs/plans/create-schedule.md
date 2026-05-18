# 일정 생성

새 일정을 등록합니다. 회원 인증이 필요하며, 일반 회원(`USER`)은 `category=GROUP_ACTIVITY`인 일정만 생성할 수 있습니다. 그 외 카테고리는 운영진(`MANAGER`)만 생성할 수 있습니다. `checkCode`를 지정하지 않으면 해당 일정은 출석 체크 대상에서 제외됩니다.

## API

```
POST /schedules
```

### 쿼리 파라미터

(해당 없음)

### 요청 바디

| 이름          |  타입  | 설명                                                                                                    |
| :------------ | :----: | :------------------------------------------------------------------------------------------------------ |
| `title`       | 문자열 | 일정 제목                                                                                               |
| `category`    | 문자열 | `CLUB`, `ACADEMIC`, `EXTERNAL`, `MANAGEMENT`, `GROUP_ACTIVITY`, `SEMINAR`, `AFTERPARTY`, `OTHER` 중 하나 |
| `location`    | 문자열 | 장소 (nullable)                                                                                         |
| `scheduledAt` | 문자열 | 시작 일시 (ISO 8601)                                                                                    |
| `endAt`       | 문자열 | 종료 일시 (ISO 8601)                                                                                    |
| `expoint`     |  숫자  | (선택, 기본 0) 출석 시 부여될 ExPoint. 0 이상                                                           |
| `checkCode`   | 문자열 | (선택) 출첵 코드. 미전송 시 null로 저장 (출석 체크 비활성 일정)                                         |

### 응답 코드 및 응답 바디

```
201 Created
```

| 이름          |  타입  | 설명                         |
| :------------ | :----: | :--------------------------- |
| `id`          |  숫자  | 생성된 일정 ID               |
| `title`       | 문자열 | 일정 제목                    |
| `category`    | 문자열 | 카테고리 enum 값             |
| `location`    | 문자열 | 장소 (nullable)              |
| `state`       | 문자열 | 일정 상태 (`public`/`trash`) |
| `scheduledAt` | 문자열 | 시작 일시 (ISO 8601)         |
| `endAt`       | 문자열 | 종료 일시 (ISO 8601)         |
| `expoint`     |  숫자  | 출석 시 부여될 ExPoint       |
| `checkCode`   | 문자열 | 출첵 코드 (nullable)         |
| `author`      |  숫자  | 작성자 회원 ID               |
| `createdAt`   | 문자열 | 생성 일시 (ISO 8601)         |

### 테스트 케이스

1. 정상 등록 → 201, 응답에 `id`/`createdAt`이 포함된다
2. `checkCode` 미전송 시 null로 저장되어 응답의 `checkCode`도 null이다
3. `checkCode` 전송 시 그 값을 그대로 저장한다
4. `endAt < scheduledAt`이면 400을 반환한다
5. 허용되지 않는 `category` 값이면 400을 반환한다
6. `expoint`가 음수이면 400을 반환한다
7. 인증되지 않은 요청 → 401
8. 일반 회원(`USER`)이 `category=GROUP_ACTIVITY`로 생성 → 201
9. 일반 회원(`USER`)이 `GROUP_ACTIVITY` 외 카테고리로 생성 시도 → 403
