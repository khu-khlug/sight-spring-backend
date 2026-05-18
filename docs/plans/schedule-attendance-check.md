# 일정 출석체크

행사 진행 중 관리자가 구두로 전달한 4자리 출첵 코드를 입력해 해당 일정에 본인을 출석 처리합니다. 회원 인증이 필요하며, 출석 시 일정에 설정된 ExPoint(`schedule.expoint`)를 적립합니다.

## API

```
POST /schedules/{scheduleId}/attendances/@me
```

### 쿼리 파라미터

(해당 없음)

### 요청 바디

| 이름   |  타입  | 설명                 |
| :----- | :----: | :------------------- |
| `code` | 문자열 | 4자리 숫자 출첵 코드 |

### 응답 코드 및 응답 바디

```
201 Created
```

| 이름             |  타입  | 설명                      |
| :--------------- | :----: | :------------------------ |
| `scheduleId`     |  숫자  | 출석한 일정 아이디        |
| `userId`         |  숫자  | 출석한 사용자 아이디      |
| `expointGranted` |  숫자  | 적립된 ExPoint (0 가능)   |
| `createdAt`      | 문자열 | 출석 처리 일시 (ISO 8601) |

### 테스트 케이스

1. 유효한 코드로 출석할 수 있다
   - `scheduledAt ≤ now ≤ endAt`, `code == schedule.checkCode`, 첫 출석 → 201, attendance row 생성, `schedule.expoint`만큼 ExPoint 적립.
2. 같은 일정에 두 번 출석체크하면 멱등 응답
   - 두 번째 호출 → 200, 기존 attendance 정보 반환, ExPoint 추가 적립 없음 (`expointGranted=0`).
3. 코드가 일치하지 않으면 401
   - 잘못된 `code` → 401, attendance row 미생성, ExPoint 미적립.
4. 출첵 시간 윈도 밖이면 400
   - `now < scheduledAt` 또는 `now > endAt` → 400.
5. `checkCode`가 `null`인 일정은 출첵 불가 → 400
6. `expoint=0` 일정도 출첵은 가능, 적립만 0
   - 정상 출석 처리, attendance row 생성, `expointGranted=0`.
7. 시도 횟수 제한 초과 → 429
   - 사용자×scheduleId 단위 분당 N회 초과.
8. 존재하지 않는 일정 → 404
