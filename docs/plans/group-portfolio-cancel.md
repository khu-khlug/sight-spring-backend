## 포트폴리오 취소 API

### 비즈니스 규칙

- **그룹장만** 취소 가능
- 취소 시 그룹 멤버 전체에게 알림

### ExPoint 변동

| 행동            | 포인트 | 대상      |
| --------------- | ------ | --------- |
| 포트폴리오 취소 | -10    | 멤버 전체 |

ExPoint 변동 시 멤버별로 `members.expoint` 직접 UPDATE + `expoint_log` INSERT 처리.

### 관련 DB 테이블

| 테이블         | 역할                                            |
| -------------- | ----------------------------------------------- |
| `group`        | `portfolio` 컬럼 UPDATE + 그룹장 여부 확인      |
| `group_member` | 전체 멤버 목록 조회 (ExPoint 일괄 처리 시 사용) |
| `members`      | `expoint` 직접 UPDATE                           |
| `expoint_log`  | ExPoint 변동 이력 INSERT                        |

### API

```
DELETE /groups/:groupId/portfolio
```

#### 응답

```
204 No Content
```

#### 테스트 케이스

1. 그룹장이 발행 중인 그룹에 요청 → `group.portfolio = false`로 변경, 멤버 전체 -10, 204 반환
2. 발행 안 된 그룹에 요청 → 404 반환
3. 그룹원(비그룹장)이 요청 → 403 반환
4. 존재하지 않는 그룹 ID 요청 → 404 반환
5. 취소 성공 시 → 그룹 멤버 전체에게 알림 발송
