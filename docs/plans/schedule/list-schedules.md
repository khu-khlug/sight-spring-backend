# 일정 목록 조회

다가오는 일정 목록을 조회합니다. 인증 없이 접근 가능하며, 응답에는 `checkCode`가 포함되지 않습니다. 현재 출석 가능한 일정은 `GET /active-schedules`를 사용합니다.

## API

```
GET /schedules
```

### 쿼리 파라미터

| 이름    |  타입  | 설명                                                               |
| :------ | :----: | :----------------------------------------------------------------- |
| `from`  | 문자열 | (선택) ISO 8601. 지정 시 이 시점 이후의 일정만 반환. 미지정 시 전체 일정 |
| `limit` |  숫자  | (선택, 기본 50, 1~50) 최대 반환 개수                               |

### 요청 바디

(해당 없음)

### 응답 코드 및 응답 바디

```
200 OK
```

| 이름                    |   타입    | 설명                         |
| :---------------------- | :-------: | :--------------------------- |
| `count`                 |   숫자    | 반환된 항목 수               |
| `schedules`             | 객체 배열 | 일정 목록                    |
| `schedules.id`          |   숫자    | 일정 ID                      |
| `schedules.title`       |  문자열   | 일정 제목                    |
| `schedules.category`    |  문자열   | 카테고리 enum 값             |
| `schedules.location`    |  문자열   | 장소 (nullable)              |
| `schedules.state`       |  문자열   | 일정 상태 (`public`/`trash`) |
| `schedules.scheduledAt` |  문자열   | 시작 일시 (ISO 8601)         |
| `schedules.endAt`       |  문자열   | 종료 일시 (ISO 8601)         |
| `schedules.expoint`     |   숫자    | 출석 시 부여될 ExPoint       |
| `schedules.author`      |   숫자    | 작성자 회원 ID               |

### 테스트 케이스

1. 정상 목록 조회 → 200, 일정 배열을 반환한다
2. 응답에 `location`/`endAt`/`expoint` 필드가 포함된다
3. 응답에 `checkCode` 필드가 포함되지 않는다
4. soft-delete된 일정은 결과에 포함되지 않는다
5. `from` 미지정 시 전체 일정을 반환한다
6. 인증 없이 접근 가능하다
