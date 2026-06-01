# 세미나 일정 삭제

세미나(`SEMINAR`) 일정을 삭제합니다 (soft-delete). 삭제된 일정은 목록/단건 조회에서 노출되지 않습니다. 세미나 일정을 삭제하면 연결된 빅세미나(`big_seminar`) 레코드도 함께 삭제됩니다. 일정을 삭제해도 해당 일정의 출석 기록(attendance)은 함께 삭제되지 않고 유지됩니다(cascade 없음). 운영진(`MANAGER`) 인증이 필요하며, 이 엔드포인트는 세미나 카테고리 일정만 삭제합니다.

> 운영진용 일반 일정은 [`delete-schedule.md`](delete-schedule.md), 그룹 활동 일정은 [`delete-group-activity-schedule.md`](delete-group-activity-schedule.md)를 참조하세요.

## API

```
DELETE /schedules/big-seminar/{scheduleId}
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
2. 세미나 일정 삭제 시 연결된 빅세미나(`big_seminar`) 레코드도 삭제된다
3. 없는 일정 → 404
4. 대상 일정이 `SEMINAR`가 아니면 → 400 (전용 엔드포인트 사용)
5. 인증되지 않은 요청 → 401
6. 일반 회원(`USER`)이 삭제 시도 → 403
7. 일정을 삭제해도 해당 일정의 출석 기록(attendance)은 삭제되지 않고 유지된다
