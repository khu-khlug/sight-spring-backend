## Schedule 도메인 변경 작업

### Schedule.kt 변경 내용

| 항목 | 변경 전 | 변경 후 |
|------|---------|---------|
| `categoryCode: Long` | 숫자 코드 저장 | `category: ScheduleCategory` + `@Enumerated(EnumType.STRING)` |
| `getCategory()` | `fromCode(categoryCode)` 헬퍼 메서드 | 제거 (직접 `category` 사용) |
| `location` | 없음 | `String?` 추가, `khlug_406`, `khlug_405`, `khlug_410`은 각 동방용으로 예약됨 |
| `expoint` | 없음 | `Int` 추가 (기본값 0) |
| `endAt` | 없음 | `LocalDateTime` 추가 |
| `checkCode` | 없음 | `String?` 추가 |

### 신규 생성 파일

없음

### ScheduleCategory.kt 재작성

기존: 숫자 코드 기반 (`val code: Long`)
```
ROOM_405(32529, "405호")  // 제거
ROOM_406(32530, "406호")  // 제거
ROOM_410(32531, "410호")  // 제거
CLUB(7742, "동아리")
ACADEMIC(7743, "학사")
EXTERNAL(7744, "외부")
```

변경 후: `label: String` 프로퍼티 추가, DB에는 enum 이름(`@Enumerated(EnumType.STRING)`) 저장
```
CLUB("동아리")
ACADEMIC("학사")
EXTERNAL("외부")
MANAGEMENT("운영")
GROUP_ACTIVITY("그룹활동")
SEMINAR("세미나")
AFTERPARTY("뒷풀이")
OTHER("기타")
```

### 영향받는 파일

| 파일 | 변경 내용 |
|------|-----------|
| `domain/schedule/ScheduleCategory.kt` | 전면 재작성 |
| `domain/schedule/Schedule.kt` | categoryCode → category, 컬럼 4개 추가, getCategory() 제거 |
| `controllers/http/dto/ListSchedulesResponse.kt` | `ScheduleCategory.fromCode(schedule.categoryCode)` → `schedule.category` |
| `service/ScheduleServiceTest.kt` | `categoryCode = 7742L` → `category = ScheduleCategory.CLUB` 등, `categoryCode = 32529L`(ROOM_405)는 유효한 variant로 교체 필요 |

### 변경 불필요한 파일

- `ScheduleController.kt` — 변경 없음
- `ScheduleService.kt` — 변경 없음
- `ScheduleRepository.kt` — 변경 없음
- `ScheduleState.kt` / `ScheduleStateConverter.kt` — 변경 없음

### 주의사항

- DB 마이그레이션 필요: `category` 컬럼을 숫자 코드에서 문자열 enum 값으로 변환
  - `7742` → `"CLUB"`, `7743` → `"ACADEMIC"`, `7744` → `"EXTERNAL"`
  - `32529`, `32530`, `32531` (ROOM_*) → 데이터 마이그레이션 전략 필요 (삭제 or `category=GROUP_ACTIVITY`+`location=khlug_*`로 처리)
