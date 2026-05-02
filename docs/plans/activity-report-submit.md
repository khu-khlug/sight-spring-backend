## 활동보고 제출 API

### 활동보고 공통 비즈니스 규칙

- **그룹장만** 제출 가능
- 제출 시 `group_seminar_term.active = 1` (접수 중)인 기수가 있어야 함 — 없으면 400
- 보고서(`presentation=false`) 또는 세미나 발표(`presentation=true`) 중 하나 선택
- 파일은 여러 개 첨부 가능 (`multipart/form-data`)
- 파일은 `files` 테이블에 저장하고 `article = group_seminar.id` 로 연결
- 등록/수정/취소(삭제) 시 그룹 멤버 전체 + 운영진에게 알림

### 관련 DB 테이블

| 테이블               | 역할                                                |
| -------------------- | --------------------------------------------------- |
| `group`              | 그룹 존재 확인 + `master` 컬럼으로 그룹장 여부 확인 |
| `group_seminar_term` | `active=1`인 접수 중 기수 존재 여부 확인            |
| `group_seminar`      | 세션 INSERT (sequence ID 채번, `presentation` 저장) |
| `files`              | 첨부파일 INSERT (`article = group_seminar.id`)      |

### API

```
POST /seminars/:seminar_term/:groupId
```

#### 요청 바디 (`multipart/form-data`)

| 이름           | 타입      | 설명                                    |
| -------------- | --------- | --------------------------------------- |
| `presentation` | boolean   | false=보고서, true=세미나 발표(default) |
| `files`        | 파일 배열 | 제출 자료 (여러 개 가능)                |

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
4. 그룹장이 접수중인 기수에 제출했지만 요청바디에 파일이 없음
   - 400 반환, 1개 이상의 파일(보고서/ppt) 제출 필수
5. 그룹원(비그룹장) 요청
   - 403 반환
6. 등록 성공 시
   - 그룹 멤버 전체 + 운영진에게 알림 발송
