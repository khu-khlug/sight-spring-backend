## 북마크 삭제 API

### 비즈니스 규칙

- 본인의 북마크만 삭제 가능
- ExPoint 변동 없음

### 관련 DB 테이블

| 테이블           | 역할                     |
| ---------------- | ------------------------ |
| `group`          | 그룹 존재 확인           |
| `group_bookmark` | 즐겨찾기 row DELETE      |

### API

```
DELETE /users/:userId/bookmark/:groupId
```

#### 응답

```
204 No Content
```

#### 테스트 케이스

1. 즐겨찾기 된 그룹에 요청 → `group_bookmark` row DELETE, 204 반환
2. 즐겨찾기 안 된 그룹에 요청 → 404 반환
3. 존재하지 않는 그룹 ID 요청 → 404 반환
4. 다른 회원의 북마크에 삭제 요청 → 403 반환
