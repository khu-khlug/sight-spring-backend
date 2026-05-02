## 활동보고 조회 API

### 활동보고 공통 비즈니스 규칙

- **그룹장만** 제출 가능
- 제출 시 `group_seminar_term.active = 1` (접수 중)인 기수가 있어야 함 — 없으면 400
- 보고서(`presentation=false`) 또는 세미나 발표(`presentation=true`) 중 하나 선택
- 파일은 여러 개 첨부 가능 (`multipart/form-data`)
- 파일은 `files` 테이블에 저장하고 `article = group_seminar.id` 로 연결
- 등록/수정/취소(삭제) 시 그룹 멤버 전체 + 운영진에게 알림

### 관련 DB 테이블

| 테이블          | 역할                                              |
| --------------- | ------------------------------------------------- |
| `group`         | 그룹 존재 확인                                    |
| `group_member`  | 요청자가 그룹 멤버인지 확인                       |
| `group_seminar` | 세션 조회                                         |
| `files`         | 첨부파일 목록 조회 (`article = group_seminar.id`) |

### API

```
GET /seminars/:seminar_term/:groupId
```

그룹 멤버 누구나 조회 가능. 운영진 조회 가능.

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

1. 존재하지 않는 `seminar_term` → 404
2. 존재하지 않는 `groupId` → 404
3. 그룹원이 아님 → 403
4. 그룹원이 조회 요청 → 200
5. 운영진이 조회 요청 → 200
