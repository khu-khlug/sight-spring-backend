# 일정 출석자 목록 조회

특정 일정에 출석한 멤버 목록을 조회합니다. 운영진(`MANAGER`)만 호출할 수 있습니다.

## API

```
GET /schedules/{scheduleId}/attendances
```

### 쿼리 파라미터

(해당 없음)

### 요청 바디

(해당 없음)

### 응답 코드 및 응답 바디

```
200 OK
```

| 이름                    |   타입    | 설명                       |
| :---------------------- | :-------: | :------------------------- |
| `count`                 |   정수    | 출석자 수                  |
| `attendances`           | 객체 배열 | 출석자 목록                |
| `attendances.userId`    |   숫자    |                            |
| `attendances.isChecked` |   불린    |                            |
| `attendances.createdAt` |  문자열   | ISO 8601                   |

### 테스트 케이스

1. 정상 조회 → 200, attendance 배열.
2. 빈 출석자 → `count=0`, `attendances=[]`.
3. 권한 없음 → 403
4. 없는 일정 → 404
