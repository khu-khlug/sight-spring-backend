## 활동보고 제출 API

### 비즈니스 규칙

- 그룹장만 제출 가능
- 제출 시 현재보다 `schedule.endAt`이 더 나중인 `big_seminar`가 있어야 함 — 없으면 400
- 보고서(`isPresentation=false`) 또는 세미나 발표(`isPresentation=true`) 중 하나 선택
- 파일 제출 필수

### 파일 검증 흐름 (`fileUploadId`)

1. `file_upload`에서 `id = fileUploadId` row 조회 — 없으면 400
2. `api_path != "/groups/:groupId/activity-report/upload-link"` — 400
3. `memberId != 요청자` — 400
4. `isVerified == true` (이미 사용된 key) — 400
5. R2에 해당 key 파일 미존재 — row 삭제 후 400 (파일이 없으면 row도 유효하지 않음)
6. 모두 통과 → `isVerified = true` 설정 후 진행 (`fileKey`는 `file_upload.fileKey` 사용)

### ExPoint 변동

| 행동          | 포인트 | 대상      |
| ------------- | ------ | --------- |
| 활동보고 제출 | +50    | 멤버 전체 |

ExPoint 변동 시 멤버별로 `members.expoint` 직접 UPDATE + `expoint_log` INSERT 처리.

### 관련 DB 테이블

| 테이블                  | 역할                                                        |
| ----------------------- | ----------------------------------------------------------- |
| `group`                 | 그룹 존재 확인 + `master` 컬럼으로 그룹장 여부 확인         |
| `big_seminar`+`schedule`| 현재로부터 가장 가까운 세미나 존재 여부 확인 (`schedule.endAt`) |
| `file_upload`           | 파일 검증 + `isVerified` UPDATE                             |
| `group_activity_report` | 활동보고 INSERT                                             |
| `group_member`          | 전체 멤버 목록 조회 (ExPoint 일괄 처리 시 사용)             |
| `members`               | `expoint` 직접 UPDATE                                       |
| `expoint_log`           | ExPoint 변동 이력 INSERT                                    |

### API

```
POST /groups/:groupId/activity-report
```

#### 요청 바디

| 이름              | 타입    | 설명                                         |
| ----------------- | ------- | -------------------------------------------- |
| `isPresentation`  | boolean | false=보고서, true=세미나 발표               |
| `fileUploadId`    | id      | 업로드 링크 발급 시 받은 `file_upload.id`    |

#### 응답

```
201 Created
```

| 이름             | 타입      | 설명                              |
| ---------------- | --------- | --------------------------------- |
| `id`             | id        | 생성된 `group_activity_report.id` |
| `groupId`        | bigint    | 제출한 그룹                       |
| `seminarId`      | id        | 해당하는 세미나                   |
| `isPresentation` | boolean   | 발표 여부                         |
| `reportFileKey`  | string    | R2 파일 key                       |
| `created_at`     | timestamp | 생성 일자                         |
| `updated_at`     | timestamp | 변경 일자                         |

#### 테스트 케이스

1. 그룹장이 접수 중인 세미나가 있을 때 보고서 제출 → `group_activity_report` INSERT, 201 반환
2. 그룹장이 접수 중인 세미나가 있을 때 세미나 발표 자료 제출 → `isPresentation=true`로 INSERT, 201 반환
3. 접수 중인 세미나(`schedule.endAt`이 현재보다 나중)가 없을 때 요청 → 400
4. 요청 바디에 `fileUploadId` 없음 → 400
5. 그룹원(비그룹장) 요청 → 403
6. 존재하지 않는 `groupId` 요청 → 404

- 파일 검증 관련
  1. `fileUploadId`에 해당하는 row 없음 → 400
  2. `api_path` 불일치 (다른 용도로 발급된 key) → 400
  3. `memberId` 불일치 (다른 사람이 발급받은 key) → 400
  4. `isVerified == true` (이미 사용된 key) → 400
  5. R2에 파일 미존재 → row 삭제 후 400
  6. 모두 통과 → `isVerified = true`, 제출 진행

7. 등록 성공 시
   - 그룹원 전원 경험치 +50
   - 그룹 멤버 전체 + 운영진에게 알림 발송
