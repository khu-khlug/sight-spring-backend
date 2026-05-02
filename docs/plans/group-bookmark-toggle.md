## 즐겨찾기 토글 API

회원별 그룹 즐겨찾기 추가/제거

### 비즈니스 규칙

- 로그인한 모든 회원 사용 가능
- `group_bookmark` 테이블 row 존재 여부로 현재 상태 판단
  - 없으면 → INSERT (추가, `bookmarked = true`)
  - 있으면 → DELETE (제거, `bookmarked = false`)
- ExPoint 변동 없음
- 추가/제거 시 본인에게 알림

### 관련 DB 테이블

| 테이블           | 역할                         |
| ---------------- | ---------------------------- |
| `group`          | 그룹 존재 확인               |
| `group_bookmark` | 즐겨찾기 row INSERT / DELETE |

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
