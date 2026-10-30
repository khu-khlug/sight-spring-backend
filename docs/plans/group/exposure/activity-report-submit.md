# 활동보고 제출

접수 중인 세미나에 그룹의 활동보고(보고서 또는 세미나 발표)를 제출합니다. 파일은 먼저 업로드 링크 발급 API로 올린 뒤 `fileUploadId`로 연결합니다.

## 운영 그룹인 경우

(해당 없음)

## API 경로

```
POST /groups/:groupId/activity-reports
```

## 권한

USER and 그룹장

## 조건

- 그룹이 종료(`SUCCESS`·`FAIL`) 상태가 아니어야 함
- 현재보다 `schedule.endAt`이 더 나중인 `big_seminar`가 있어야 함
- 보고서(`isPresentation=false`) 또는 세미나 발표(`isPresentation=true`) 중 하나 선택
- 파일 제출 필수
- `fileUploadId`가 아래 파일 검증 흐름을 통과해야 함

### 파일 검증 흐름 (`fileUploadId`)

1. `file_upload`에서 `id = fileUploadId` row 조회 — 없으면 400
2. `api_path != "/groups/:groupId/activity-reports/upload-link"` — 400
3. `memberId != 요청자` — 400
4. `isUploaded == true` (이미 사용된 key) — 400
5. 이미 삭제 처리된 파일 (삭제 대기 중이라 R2에는 아직 남아 있을 수 있음) — 400
6. R2에 해당 key 파일 미존재 — row 삭제 후 400 (파일이 없으면 row도 유효하지 않음)
7. 모두 통과 → `isUploaded = true` 설정 후 진행 (`fileKey`는 `file_upload.fileKey` 사용)

## 부수 효과

### 그룹 상태

실제 변경이 발생하면 중단 상태를 진행 중으로 자동 전환한다. [그룹 공통 사항](../global.md#활동에-따른-중단-상태-해제)을 따른다.


### 경험치 변동

| 행동          | 포인트 | 대상      |
| ------------- | ------ | --------- |
| 세미나 발표 등록(`isPresentation=true`) | +150 | 멤버 전체 |
| 보고서 제출(`isPresentation=false`) | +150 | 멤버 전체 |

ExPoint 변동 시 멤버별로 `members.expoint` 직접 UPDATE + `expoint_log` INSERT 처리.

- 세미나 발표와 보고서 모두 제출 시 멤버 전체에게 +150 exp를 지급한다.

### 알림

그룹 멤버 전체 + 운영진에게 알림 발송

### 그룹 활동 로그

그룹 활동 로그에 활동보고 제출을 기록하고 최근 활동 일시를 갱신함

## 쿼리스트링

(해당 없음)

## 요청 바디

| 이름              | 타입    | 설명                                         |
| ----------------- | ------- | -------------------------------------------- |
| `isPresentation`  | boolean | false=보고서, true=세미나 발표               |
| `fileUploadId`    | 문자열 | 업로드 링크 발급 시 받은 `file_upload.id`    |

## 성공 응답

```
201 Created
```

| 이름             | 타입      | 설명                              |
| ---------------- | --------- | --------------------------------- |
| `id`             | 문자열 | 생성된 `group_activity_report.id` |
| `groupId`        | 숫자(Long) | 제출한 그룹                       |
| `seminarId`      | 문자열 | 해당하는 세미나                   |
| `isPresentation` | boolean   | 발표 여부                         |
| `reportFileKey`  | string    | R2 파일 key                       |
| `createdAt`      | string    | 생성 일시 (ISO 8601 UTC, `Z` 접미사) |
| `updatedAt`      | string    | 변경 일시 (ISO 8601 UTC, `Z` 접미사) |

## 실패 응답

| 상황                                                    | 응답 코드 |
| :------------------------------------------------------ | :-------: |
| 접수 중인 세미나(`schedule.endAt`이 현재보다 나중)가 없음 |    400    |
| `fileUploadId`가 없거나 파일 검증 흐름을 통과하지 못함   |    400    |

## 테스트 케이스

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
  4. `isUploaded == true` (이미 사용된 key) → 400
  5. 이미 삭제 처리된 파일 (삭제 대기 중이라 R2에는 아직 남아 있을 수 있음) → 400
  6. R2에 파일 미존재 → row 삭제 후 400
  7. 모두 통과 → `isUploaded = true`, 제출 진행

7. 등록 성공 시
   - 세미나 발표·보고서 모두 그룹원 전원 +150 exp
   - 그룹 멤버 전체 + 운영진에게 알림 발송

## 관련 DB 테이블

| 테이블                  | 역할                                                        |
| ----------------------- | ----------------------------------------------------------- |
| `group`                 | 그룹 존재 확인 + `master` 컬럼으로 그룹장 여부 확인         |
| `big_seminar`+`schedule`| 현재로부터 가장 가까운 세미나 존재 여부 확인 (`schedule.endAt`) |
| `file_upload`           | 파일 검증 + `isUploaded` UPDATE                             |
| `group_activity_report` | 활동보고 INSERT                                             |
| `group_member`          | 전체 멤버 목록 조회 (ExPoint 일괄 처리 시 사용)             |
| `members`               | `expoint` 직접 UPDATE                                       |
| `expoint_log`           | ExPoint 변동 이력 INSERT                                    |
