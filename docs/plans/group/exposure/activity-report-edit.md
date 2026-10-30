# 접수중인 활동보고 수정

이미 제출한 활동보고의 유형(보고서/세미나 발표)이나 파일을 바꿉니다.

## 운영 그룹인 경우

(해당 없음)

## API 경로

```
PATCH /groups/:groupId/activity-reports/:reportId
```

## 권한

USER and 그룹장

## 조건

- 그룹이 종료(`SUCCESS`·`FAIL`) 상태가 아니어야 함
- 해당 활동보고에 연결된 세미나의 `schedule.endAt`이 지나지 않았어야 함
- `isPresentation`, `fileUploadId` 중 하나 이상 전송해야 함
- `fileUploadId`를 전송하면 아래 파일 검증 흐름을 통과해야 함

## 파일 검증 흐름 (`fileUploadId` 변경 시)

1. `file_upload`에서 `id = fileUploadId` row 조회 — 없으면 400
2. `api_path != "/groups/:groupId/activity-reports/upload-link"` — 400
3. `memberId != 요청자` — 400
4. `isUploaded == true` (이미 사용된 key) — 400
5. 이미 삭제 처리된 파일 (삭제 대기 중이라 R2에는 아직 남아 있을 수 있음) — 400
6. R2에 해당 key 파일 미존재 — row 삭제 후 400 (파일이 없으면 row도 유효하지 않음)
7. 모두 통과 → 기존 파일 삭제 표시 + `isUploaded = true` 설정 후 진행. 기존 R2 파일은 커밋 후 [파일 삭제 흐름](../global.md#삭제-흐름)으로 지움 (`fileKey`는 `file_upload.fileKey` 사용)

## 부수 효과

### 그룹 상태

실제 변경이 발생하면 중단 상태를 진행 중으로 자동 전환한다. [그룹 공통 사항](../global.md#활동에-따른-중단-상태-해제)을 따른다.


### 경험치 변동

(해당 없음)

### 알림

- `isPresentation`만 변경 → 그룹 멤버 전체 + 운영진에게 발표 여부 변경 알림
- `fileUploadId`만 전송 → 그룹 멤버 전체 + 운영진에게 제출 파일 변경 알림
- 둘 다 전송 → 그룹 멤버 전체 + 운영진에게 발표 여부 및 제출 파일 변경 알림

### 그룹 활동 로그

그룹 활동 로그에 활동보고 수정을 기록하고 최근 활동 일시를 갱신함

## 쿼리스트링

(해당 없음)

## 요청 바디

| 이름             | 타입               | 기본값          | 설명                               |
| ---------------- | ------------------ | --------------- | ---------------------------------- |
| `isPresentation` | boolean (nullable) | null(변경 안 함) | false=보고서, true=세미나 발표     |
| `fileUploadId`   | 문자열 또는 null | null(변경 안 함) | 파일 변경 시 전송                  |

## 성공 응답

```
200 OK
```

| 이름             | 타입      | 설명                            |
| ---------------- | --------- | ------------------------------- |
| `id`             | 문자열 | `group_activity_report.id`      |
| `groupId`        | 숫자(Long) | group.id                        |
| `seminarId`      | 문자열 | big_seminar.id                  |
| `isPresentation` | boolean   | 발표 여부                       |
| `reportFileKey`  | string    | 보고 파일 key                   |
| `createdAt`      | string    | 생성 일시 (ISO 8601 UTC, `Z` 접미사) |
| `updatedAt`      | string    | 변경 일시 (ISO 8601 UTC, `Z` 접미사) |

## 실패 응답

| 상황                                                   | 응답 코드 |
| :----------------------------------------------------- | :-------: |
| 접수 기간(`schedule.endAt`)이 지난 활동보고임          |    400    |
| `isPresentation`, `fileUploadId` 모두 null/미전송      |    400    |
| `fileUploadId`가 파일 검증 흐름을 통과하지 못함        |    400    |

## 테스트 케이스

1. 그룹원(비그룹장) 수정 요청 → 403
2. 존재하지 않는 `reportId` 수정 요청 → 404
3. 접수 기간(`schedule.endAt`)이 지난 활동보고 수정 요청 → 400
4. `isPresentation`, `fileUploadId` 모두 null/미전송 → 400
5. `isPresentation`만 전송 → `isPresentation` 업데이트, 200 반환
6. `fileUploadId`만 전송 → 파일 검증 통과 시 기존 R2 파일 삭제 + `reportFileKey` 업데이트, 200 반환
7. 둘 다 전송 → 파일 검증 통과 시 기존 R2 파일 삭제 + 둘 다 업데이트, 200 반환

- 파일 검증 관련 (`fileUploadId` 전송 시)
  1. `fileUploadId`에 해당하는 row 없음 → 400
  2. `api_path` 불일치 (다른 용도로 발급된 key) → 400
  3. `memberId` 불일치 (다른 사람이 발급받은 key) → 400
  4. `isUploaded == true` (이미 사용된 key) → 400
  5. 이미 삭제 처리된 파일 (삭제 대기 중이라 R2에는 아직 남아 있을 수 있음) → 400
  6. R2에 파일 미존재 → row 삭제 후 400
  7. 모두 통과 → 기존 파일 삭제 표시 + `isUploaded = true`, 수정 진행, 커밋 후 기존 R2 파일 삭제

- 알림 관련
  1. `isPresentation`만 변경 시 → 그룹 멤버 전체 + 운영진에게 발표 여부 변경 알림
  2. `fileUploadId`만 전송 시 → 그룹 멤버 전체 + 운영진에게 제출 파일 변경 알림
  3. 둘 다 전송 시 → 그룹 멤버 전체 + 운영진에게 발표 여부 및 제출 파일 변경 알림

## 관련 DB 테이블

| 테이블                  | 역할                                                |
| ----------------------- | --------------------------------------------------- |
| `group`                 | 그룹 존재 확인 + `master` 컬럼으로 그룹장 여부 확인 |
| `group_activity_report` | 활동보고 조회 및 UPDATE                             |
| `big_seminar`+`schedule`| 접수 기간 내 여부 확인 (`schedule.endAt`)           |
| `file_upload`           | 파일 검증 + `isUploaded` UPDATE (`fileUploadId` 변경 시) |
