## 접수중인 활동보고 수정 API

### 비즈니스 규칙

- 그룹장만 수정 가능
- 해당 활동보고에 연결된 세미나의 `schedule.endAt`이 지나지 않았어야 함 — 지났으면 400
- `isPresentation`, `reportFile` 모두 null/미전송이면 400
- `reportFile` 변경 시 `r2FileUploadId`도 필수

### 파일 검증 흐름 (`reportFile` 변경 시)

1. `r2_file_upload`에서 `id = r2FileUploadId` row 조회 — 없으면 400
2. `key != reportFile` — 400
3. `api_path != "/group/:groupId/activity-report/upload-link"` — 400
4. `memberId != 요청자` — 400
5. `isVerified == true` (이미 사용된 key) — 400
6. R2에 해당 key 파일 미존재 — row 삭제 후 400 (파일이 없으면 row도 유효하지 않음)
7. 모두 통과 → 기존 R2 파일 삭제 + `isVerified = true` 설정 후 진행

### 관련 DB 테이블

| 테이블                  | 역할                                                |
| ----------------------- | --------------------------------------------------- |
| `group`                 | 그룹 존재 확인 + `master` 컬럼으로 그룹장 여부 확인 |
| `group_activity_report` | 활동보고 조회 및 UPDATE                             |
| `big_seminar`+`schedule`| 접수 기간 내 여부 확인 (`schedule.endAt`)           |
| `r2_file_upload`        | 파일 검증 + `isVerified` UPDATE (`reportFile` 변경 시) |

### API

```
PATCH /group/:groupId/activity-report/:reportId
```

#### 요청 바디

| 이름             | 타입               | 설명                               |
| ---------------- | ------------------ | ---------------------------------- |
| `isPresentation` | boolean (nullable) | false=보고서, true=세미나 발표     |
| `reportFile`     | string (nullable)  | 변경할 파일의 R2 key               |
| `r2FileUploadId` | id (nullable)      | `reportFile` 변경 시 필수          |

**알림 규칙**:

- `isPresentation`만 변경 → 그룹 멤버 전체 + 운영진에게 발표 여부 변경 알림
- `reportFile`만 변경 → 그룹 멤버 전체 + 운영진에게 제출 파일 변경 알림
- 둘 다 변경 → 그룹 멤버 전체 + 운영진에게 발표 여부 및 제출 파일 변경 알림

#### 응답

```
200 OK
```

| 이름             | 타입      | 설명                            |
| ---------------- | --------- | ------------------------------- |
| `id`             | id        | `group_activity_report.id`      |
| `groupId`        | bigint    | group.id                        |
| `seminarId`      | id        | big_seminar.id                  |
| `isPresentation` | boolean   | 발표 여부                       |
| `reportFile`     | string    | 보고 파일 R2 key                |
| `created_at`     | timestamp | 생성 일자                       |
| `updated_at`     | timestamp | 변경 일자                       |

#### 테스트 케이스

1. 그룹원(비그룹장) 수정 요청 → 403
2. 존재하지 않는 `reportId` 수정 요청 → 404
3. 접수 기간(`schedule.endAt`)이 지난 활동보고 수정 요청 → 400
4. `isPresentation`, `reportFile` 모두 null/미전송 → 400
5. `reportFile` 전송 시 `r2FileUploadId` 미전송 → 400
6. `isPresentation`만 전송 → `isPresentation` 업데이트, 200 반환
7. `reportFile`만 전송 → 파일 검증 통과 시 기존 R2 파일 삭제 + `reportFile` 업데이트, 200 반환
8. 둘 다 전송 → 파일 검증 통과 시 기존 R2 파일 삭제 + 둘 다 업데이트, 200 반환

- 파일 검증 관련 (`reportFile` 변경 시)
  1. `r2FileUploadId`에 해당하는 row 없음 → 400
  2. `r2FileUploadId`와 `reportFile`(key) 불일치 → 400
  3. `api_path` 불일치 (다른 용도로 발급된 key) → 400
  4. `memberId` 불일치 (다른 사람이 발급받은 key) → 400
  5. `isVerified == true` (이미 사용된 key) → 400
  6. R2에 파일 미존재 → row 삭제 후 400
  7. 모두 통과 → 기존 R2 파일 삭제 + `isVerified = true`, 수정 진행

- 알림 관련
  1. `isPresentation`만 변경 시 → 그룹 멤버 전체 + 운영진에게 발표 여부 변경 알림
  2. `reportFile`만 변경 시 → 그룹 멤버 전체 + 운영진에게 제출 파일 변경 알림
  3. 둘 다 변경 시 → 그룹 멤버 전체 + 운영진에게 발표 여부 및 제출 파일 변경 알림
