# 방별 일일 방문자 수 조회

동방 출입 시(`POST /internal/door-lock/accesses`) 넘어오는 `roomNumber`가 실제 존재하는 방 번호인지 검증하고, (날짜, 방, 회원) 단위로 방문 기록을 저장해 하루에 각 방을 몇 명이 방문했는지(같은 사람 중복 방문 제외) 조회할 수 있게 한다.

## API

```
GET /internal/door-lock/daily-visit-count?room={roomNumber}
```
권한: SYSTEM

특정 방에 특정 날짜(기본값 오늘) 기준으로 몇 명이 방문했는지(중복 제외) 반환한다. 

### 쿼리 파라미터

| 이름   |  타입  | 설명                                          |
| :----- | :----: | :--------------------------------------------- |
| `room` | uint   | 조회할 방 번호 |

### 요청 바디

(해당 없음)

### 응답 코드 및 응답 바디

```
200 OK
```

| 이름   |  타입  | 설명                      |
| :----- | :----: | :--------------------------------------------- |
| `count` |  uint  | 그날 그 방을 방문한 인원 수(중복 회원 제외) |

### 테스트 케이스

1. 아무도 방문하지 않은 날짜/방 조회 → `count: 0`
2. 같은 회원이 같은 날 같은 방을 3번 출입해도 `count`는 1
3. 같은 회원이 같은 날 다른 방을 1번씩 방문하면 각 방의 `count`는 1
4. 서로 다른 회원 3명이 같은 날 같은 방을 방문하면 `count: 3`
5. 유효하지 않은 `roomNumber`(405/406/410 아님) 조회 시 400 에러
6. SYSTEM 권한 없이 요청하면 403

## 기존 API 변경: `POST /internal/door-lock/accesses`

`roomNumber`를 더 이상 양수 여부만 검사하지 않고, 실제 존재하는 방 번호(405, 406, 410)인지
검사한다. 유효하지 않으면 400을 반환한다. 요청/응답 스키마 자체는 바뀌지 않는다.

### 테스트 케이스

1. `roomNumber`가 405/406/410 중 하나가 아니면(예: 0, -1, 999) `BadRequestException`(400)
2. 유효한 `roomNumber`로 같은 회원이 같은 날 두 번 요청해도 정상 처리되고(기존 동작 유지), 방문 기록은 한 번만 쌓인다
3. 같은 회원이 같은 날 다른 방으로 요청하면 방문 기록이 각각 쌓인다(방 단위로 구분)


## 코드 추가/변경

### **`com.sight.domain.room.DailyVisitLog`** (`domain/room`)
- 필드:
  - `date`: KST
  - `roomNumber`: Int
  - `memberId`: Long
- `@IdClass` 키(`DailyVisitLogId`)는 `roomNumber`(Int) + `memberId`(Long)
- 나머지 필드는 `var ... private set`, `visit(date)`로만 갱신
- 이미 그 방, 그 멤버의 기록이 있으면 날짜를 덮어쓴다.
- 테이블명: `daily_visit_log`

### **`com.sight.repository.DailyVisitLogRepository`**
- `JpaRepository<DailyVisitLog, DailyVisitLogId>` 상속이라 `save()`는 이미 있음  
  신규 생성/ 기존 갱신 둘 다 이걸로 처리(`@IdClass` 키 기준으로 JPA가 알아서 INSERT/UPDATE 결정).
- `findByRoomNumberAndMemberId(roomNumber, memberId): DailyVisitLog?`  
  저장 전 기존 기록 조회용  
  (있으면 그 엔티티의 `visit(date)`를 호출해 날짜 갱신 후 `save()`, 없으면 새로 만들어 `save()`)
- `countByDateAndRoomNumber(date, roomNumber): Long`  
  조회 API가 쓸 집계

### **`com.sight.core.room`** (신규 패키지) — 방 번호 공용 상수
```kotlin
val CLUB_ROOM_LOCATIONS = setOf(405, 406, 410)
```
`Int`로 둔다 — 문자열이어야 할 이유가 없고, 도어락 쪽(`roomNumber: Int`)은 변환 없이 바로 비교
가능해진다. 대신 `ScheduleService`(`location: String?`)에서 비교할 때
`location.toIntOrNull() in CLUB_ROOM_LOCATIONS`로 바꾼다(숫자가 아닌 장소는 자연스럽게
`false`).

`ScheduleService`(장소 검증, 지금은 자체 `CLUB_ROOM_LOCATIONS`)와 새 `RoomService` 둘 다 여기
걸 참고하도록 바꾼다. `ScheduleService.CLUB_ROOM_LOCATIONS`(private, 파일 내부 전용)는 지우고
이 공용 상수를 import해서 쓴다.

### **`RoomService`** (신규, `com.sight.service`)
- `getDailyVisitCount(roomNumber): Long`  
  `roomNumber`가 `CLUB_ROOM_LOCATIONS`에 없으면 `BadRequestException`, 있으면 `DailyVisitLogRepository.countByDateAndRoomNumber`로 오늘 날짜 기준 집계

### **`InternalDoorLockController`에 엔드포인트 추가**
- `GET /internal/door-lock/daily-visit-count`  
  SYSTEM 권한, 기존 `listDoorLockMembers`와 같은 컨트롤러  
  `room` 쿼리 파라미터를 받아 `RoomService.getDailyVisitCount(room)` 호출 후 `{ count }` 반환

### **`DoorLockAccessService.createDoorLockAccess` 수정**
- `roomNumber` 검증 로직 추가  
  `com.sight.core.room.CLUB_ROOM_LOCATIONS` 참고  
  (`roomNumber in CLUB_ROOM_LOCATIONS`, 변환 불필요)
- 출입 처리 후 `DailyVisitLogRepository.findByRoomNumberAndMemberId`로 기존 기록을 찾아서
  - 있으면 `visit(오늘)` 호출 후 `save()`로 갱신
  - 없으면 오늘 날짜로 새 `DailyVisitLog`를 생성해 `save()`
