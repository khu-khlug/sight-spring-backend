# 출석 기록 삭제

운영진이 잘못 처리된 출석 기록을 삭제합니다. 이미 적립된 ExPoint를 회수합니다.

## API

```
DELETE /schedules/{scheduleId}/attendances/{userId}
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

1. 정상 삭제 → 204. attendance row 삭제. 적립 ExPoint(`schedule.expoint`) 회수.
2. ExPoint 회수는 transaction 시스템 통한 음수 적립으로 기록된다.
3. 처음부터 출석한 적 없음 → 404
4. 권한 없음 → 403
5. 일정 자체가 없거나 삭제됨 → 404
6. 같은 `(scheduleId, userId)`에 두 번 DELETE → 첫 번째 204, 두 번째 404
