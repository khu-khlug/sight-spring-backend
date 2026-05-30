# 일정 삭제 (운영진)

운영진(`MANAGER`)이 일반 일정을 삭제합니다 (soft-delete). 삭제된 일정은 목록/단건 조회에서 노출되지 않습니다. 일정을 삭제해도 해당 일정의 출석 기록(attendance)은 함께 삭제되지 않고 유지됩니다(cascade 없음). 운영진 인증이 필요하며, 이 엔드포인트는 운영진 카테고리(`CLUB`, `ACADEMIC`, `EXTERNAL`, `MANAGEMENT`, `AFTERPARTY`, `OTHER`) 일정만 삭제합니다.

> 그룹 활동 일정은 [`delete-group-activity-schedule.md`](delete-group-activity-schedule.md), 세미나 일정은 [`delete-big-seminar-schedule.md`](delete-big-seminar-schedule.md)를 참조하세요.

## API

```
DELETE /schedules/{scheduleId}
```

### 쿼리 파라미터

(해당 없음)

### 요청 바디

(해당 없음)

### 응답 코드 및 응답 바디

```
204 No Content
```

응답 본문 없음.

### 테스트 케이스

1. 정상 삭제 → 204. 이후 목록/단건 조회에서 노출되지 않는다
2. 없는 일정 → 404
3. 대상 일정이 운영진 카테고리가 아니면(예: `GROUP_ACTIVITY`/`SEMINAR`) → 400 (전용 엔드포인트 사용)
4. 인증되지 않은 요청 → 401
5. 일반 회원(`USER`)이 삭제 시도 → 403
6. 일정을 삭제해도 해당 일정의 출석 기록(attendance)은 삭제되지 않고 유지된다
