## 접수중인 활동보고 제출 취소 API

### 활동보고 공통 비즈니스 규칙

- **그룹장만** 제출 가능
- 제출 시 `group_seminar_term.active = 1` (접수 중)인 기수가 있어야 함 — 없으면 400
- 보고서(`presentation=false`) 또는 세미나 발표(`presentation=true`) 중 하나 선택
- 파일은 여러 개 첨부 가능 (`multipart/form-data`)
- 파일은 `files` 테이블에 저장하고 `article = group_seminar.id` 로 연결
- 등록/수정/취소(삭제) 시 그룹 멤버 전체 + 운영진에게 알림

### 관련 DB 테이블

| 테이블          | 역할                                                |
| --------------- | --------------------------------------------------- |
| `group`         | 그룹 존재 확인 + `master` 컬럼으로 그룹장 여부 확인 |
| `group_seminar` | 세션 DELETE                                         |
| `files`         | 첨부파일 DELETE (`article = group_seminar.id`)      |

### API

```
DELETE /seminars/:seminar_term/:groupId
```

#### 응답

```
204 No Content
```

#### 테스트 케이스

1. 존재하지 않는 `seminar_term` → 404
2. 접수 중이 아닌 `seminar_term` → 400
3. 해당 기수에 해당 `groupId`로 제출된 활동보고가 없음 → 404
4. 그룹원(비그룹장, 비운영진) 삭제 요청 → 403
5. 그룹장이 삭제 요청 → 204
6. 운영진이 삭제 요청 → 204
7. 삭제 성공 시
   - 해당 활동보고 및 첨부파일 삭제
   - 그룹 멤버 전체 + 운영진에게 알림 발송
