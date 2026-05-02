# 그룹 관련 사이드 기능들 API

---

## 기능 목록

| 기능                 | 설명                                                      |
| -------------------- | --------------------------------------------------------- |
| 포트폴리오 발행 토글 | 그룹장이 그룹 포트폴리오를 발행/취소; 전 멤버 ExPoint ±10 |
| 즐겨찾기 토글        | 회원별 그룹 즐겨찾기 추가/제거                            |
| 활동보고             | 접수 중인 세미나에 세션 등록                              |

---

## 공통 참고

### ExPoint 변동

| 행동            | 포인트 | 대상      |
| --------------- | ------ | --------- |
| 포트폴리오 발행 | +10    | 멤버 전체 |
| 포트폴리오 취소 | -10    | 멤버 전체 |

ExPoint 변동 시 `members.expoint` 직접 UPDATE + `expoint_log` INSERT 처리.

### 관련 DB 테이블

| 테이블               | 역할                                                                     |
| -------------------- | ------------------------------------------------------------------------ |
| `group`              | 그룹 본체 (`portfolio` 컬럼으로 발행 여부 관리)                          |
| `group_member`       | 그룹-멤버 연결 (`group`, `member` 컬럼) — 전체 멤버 ExPoint 처리 시 조회 |
| `group_bookmark`     | 즐겨찾기 (`member`, `group` 복합 PK)                                     |
| `group_seminar`      | 세미나 세션 등록 + 활동보고 (`id` 추가, `presentation` 추가)             |
| `group_seminar_term` | 세미나 기수 (`active=1`이면 접수 중)                                     |
| `files`              | 파일 첨부 (`article = group_seminar.id`)                                 |
| `members`            | 회원 (`expoint` 컬럼)                                                    |
| `expoint_log`        | ExPoint 변동 이력                                                        |

---

## 1. 포트폴리오 발행 토글

### 비즈니스 규칙

- **그룹장만** 발행/취소 가능
- `group.portfolio` 컬럼을 토글 (0 ↔ 1)
- 발행 → 전 멤버 +10 ExPoint, 취소 → 전 멤버 -10 ExPoint
- ExPoint 변동 내역은 `expoint_log`에 각 멤버별로 INSERT
- 발행/취소 시 그룹 멤버 전체에게 알림

### API

```
POST /groups/:groupId/portfolio
```

#### 쿼리 파라미터

(해당 없음)

#### 요청 바디

(해당 없음)

#### 응답

```
200 OK
```

| 이름        | 타입    | 설명                   |
| ----------- | ------- | ---------------------- |
| `published` | boolean | 토글 후 현재 발행 여부 |

#### 테스트 케이스

1. 그룹장이 미발행 그룹에 요청
   - `group.portfolio = true`로 변경, 멤버 전체 +10
2. 그룹장이 발행 중인 그룹에 재요청
   - `group.portfolio = false`로 변경, 멤버 전체 -10
3. 그룹원(비그룹장)이 요청
   - 403 반환
4. 존재하지 않는 그룹 ID 요청
   - 404 반환
5. 발행/취소 성공 시
   - 그룹 멤버 전체에게 알림 발송

---

## 2. 즐겨찾기 토글

### 비즈니스 규칙

- 로그인한 모든 회원 사용 가능
- `group_bookmark` 테이블 row 존재 여부로 현재 상태 판단
  - 없으면 → INSERT (추가, `bookmarked = true`)
  - 있으면 → DELETE (제거, `bookmarked = false`)
- ExPoint 변동 없음
- 추가/제거 시 본인에게 알림

### API

```
POST /groups/:groupId/bookmark
```

#### 쿼리 파라미터

(해당 없음)

#### 요청 바디

(해당 없음)

#### 응답

```
200 OK
```

| 이름         | 타입    | 설명                       |
| ------------ | ------- | -------------------------- |
| `bookmarked` | boolean | 토글 후 현재 즐겨찾기 여부 |

#### 테스트 케이스

1. 즐겨찾기 안 된 그룹에 요청
   - `group_bookmark` row INSERT, `bookmarked = true` 반환
2. 즐겨찾기 된 그룹에 재요청
   - `group_bookmark` row DELETE, `bookmarked = false` 반환
3. 존재하지 않는 그룹 ID 요청
   - 404 반환
4. 추가/제거 성공 시
   - 본인에게 알림 발송

---

## 3. 활동보고

### DB 스키마 변경 필요!!

`group_seminar`에 컬럼 추가:

| 컬럼                  | 타입                   | 설명                                  |
| --------------------- | ---------------------- | ------------------------------------- |
| `id` (추가)           | bigint (sequence 채번) | 전역 고유 ID — `files.article`에 사용 |
| `group`               | bigint                 | `group.id`                            |
| `term`                | varchar                | `group_seminar_term.term`             |
| `presentation` (추가) | boolean                | false=보고서, true=세미나 발표        |
| `created_at`          | timestamp              |                                       |

### 비즈니스 규칙

- **그룹장만** 제출 가능
- 제출 시 `group_seminar_term.active = 1` (접수 중)인 기수가 있어야 함 — 없으면 400
- 보고서(`presentation=false`) 또는 세미나 발표(`presentation=true`) 중 하나 선택
- 파일은 여러 개 첨부 가능 (`multipart/form-data`)
- 파일은 `files` 테이블에 저장하고 `article = group_seminar.id` 로 연결
- 등록/수정/취소(삭제) 시 그룹 멤버 전체 + 운영진에게 알림

### API - 세션 등록

```
POST /seminars/:seminar_term/:groupId
```

#### 요청 바디 (`multipart/form-data`)

| 이름           | 타입      | 설명                           |
| -------------- | --------- | ------------------------------ |
| `presentation` | boolean   | false=보고서, true=세미나 발표 |
| `files`        | 파일 배열 | 제출 자료 (여러 개 가능)       |

#### 응답

```
201 Created
```

| 이름           | 타입        | 설명                      |
| -------------- | ----------- | ------------------------- |
| `id`           | 정수        | 생성된 `group_seminar.id` |
| `term`         | 문자열      | 제출된 기수 코드 (YYYYSS) |
| `presentation` | boolean     | 발표 여부                 |
| `files`        | 문자열 배열 | 업로드된 파일명 목록      |

#### 테스트 케이스

1. 그룹장이 접수 중인 기수가 있을 때 보고서 제출
   - `group_seminar` INSERT (sequence ID 채번), `files` INSERT, 201 반환
2. 그룹장이 접수 중인 기수가 있을 때 세미나 발표 자료 제출
   - `presentation = true`로 동일 처리
3. 접수 중인 기수가 없을 때 요청
   - 400 반환 ("접수 중인 세미나가 없습니다")
4. 그룹원(비그룹장) 요청
   - 403 반환
5. 등록 성공 시
   - 그룹 멤버 전체 + 운영진에게 알림 발송

### API - 세션 내용 조회

```
GET /seminars/:seminar_term/:groupId
```

그룹 멤버 누구나 조회 가능.

#### 응답

```
200 OK
```

| 이름           | 타입        | 설명               |
| -------------- | ----------- | ------------------ |
| `id`           | 정수        | `group_seminar.id` |
| `term`         | 문자열      | 기수 코드 (YYYYSS) |
| `presentation` | boolean     | 발표 여부          |
| `files`        | 문자열 배열 | 첨부 파일명 목록   |

#### 테스트 케이스

1. 그룹원이 조회 요청 → 200
2. 존재하지 않는 세션 요청 → 404

### API - 세션 수정

```
PATCH /seminars/:seminar_term/:groupId
```

#### 요청 바디 (`multipart/form-data`)

텍스트 파트 (JSON):

| 이름           | 타입                   | 설명                           |
| -------------- | ---------------------- | ------------------------------ |
| `presentation` | boolean (nullable)     | false=보고서, true=세미나 발표 |
| `deleteFiles`  | 문자열 배열 (nullable) | 삭제할 파일명 목록             |

바이너리 파트:

| 이름    | 타입                 | 설명                                                      |
| ------- | -------------------- | --------------------------------------------------------- |
| `files` | 파일 배열 (nullable) | 추가/대체할 파일 (기존 파일명과 겹치면 대체, 아니면 추가) |

**파일 처리 규칙**:

- `deleteFiles`에 포함된 파일명 → 삭제
- `files`에 포함된 파일, 기존 파일명과 겹치지 않음 → 추가
- `files`에 포함된 파일, 기존 파일명과 겹침 → 대체
- 언급되지 않은 기존 파일 → 유지

**알림 규칙**:

- `presentation`만 변경 → 그룹 멤버 전체 + 운영진에게 발표 여부 변경 알림
- 파일만 변경 → 그룹 멤버 전체 + 운영진에게 제출 파일 변경 알림
- 둘 다 변경 → 그룹 멤버 전체 + 운영진에게 발표 여부 및 제출 파일 변경 알림

#### 응답

```
200 OK
```

| 이름           | 타입        | 설명                |
| -------------- | ----------- | ------------------- |
| `id`           | 정수        | `group_seminar.id`  |
| `term`         | 문자열      | 기수 코드           |
| `presentation` | boolean     | 발표 여부           |
| `files`        | 문자열 배열 | 변경 후 파일명 목록 |

```
400 Bad Request
```

`presentation`, `deleteFiles`, `files` 모두 null/미전송인 경우

#### 테스트 케이스

1. 그룹원(비그룹장) 수정 요청 → 403
2. 존재하지 않는 세션 수정 요청 → 404
3. `presentation`, `deleteFiles`, `files` 모두 null/미전송 → 400
4. `deleteFiles`가 존재하는 파일명이면 → 해당 파일 삭제
5. `deleteFiles`가 존재하지 않는 파일명이면 → 무시
   (다른 수정 없이 `deleteFiles`만 요청이 왔고 모든 `deleteFiles` 파일명이 존재하지 않는 경우에도 200을 반환하게 됨.)
6. `files`에 기존 파일명과 겹치는 파일 → 대체
7. `files`에 기존 파일명과 겹치지 않는 파일 → 추가
8. `deleteFiles`와 `files` 바이너리에 동일한 파일명이 있는 경우 → 삭제를 무시하고 추가/대체로 동작
9. `presentation`만 변경 시 → 그룹 멤버 전체 + 운영진에게 발표 여부 변경 알림
10. 파일만 변경 시 → 그룹 멤버 전체 + 운영진에게 제출 파일 변경 알림
11. `presentation`과 파일 모두 변경 시 → 그룹 멤버 전체 + 운영진에게 발표 여부 및 제출 파일 변경 알림

### API - 세션 삭제

```
DELETE /seminars/:seminar_term/:groupId
```

#### 응답

```
204 No Content
```

#### 테스트 케이스

1. 그룹원(비그룹장, 비운영진) 삭제 요청 → 403
2. 존재하지 않는 세션 삭제 요청 → 404
3. 그룹장이 삭제 요청 → 204
4. 운영진이 삭제 요청 → 204
5. 삭제 성공 시
   - 그룹 멤버 전체 + 운영진에게 알림 발송

---
