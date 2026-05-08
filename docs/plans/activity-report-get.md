## 활동보고 조회 API

### 활동보고 공통 비즈니스 규칙

- 그룹장만 제출 가능
- 제출 시 현재보다 `endAt`이 더 나중인 `big_seminar`가 있어야 함 — 없으면 400
- 보고서(`presentation=false`) 또는 세미나 발표(`presentation=true`) 중 하나 선택
- 파일 제출 필수
- 파일은 R2에 저장하고 해당파일의 url을 요청바디에 넣어 제출
- 등록/수정/취소(삭제) 시 그룹 멤버 전체 + 운영진에게 알림

### 관련 DB 테이블

| 테이블          | 역할                                              |
| --------------- | ------------------------------------------------- |
| `group`         | 그룹 존재 확인                                    |
| `group_member`  | 요청자가 그룹 멤버인지 확인                       |
| `group_activity_report` | 활동 보고 조회                          |
| `big_seminar`| 세미나 정보조회|

### API

```
GET /group/:groupId/activity-report
```

그룹 멤버 누구나 조회 가능. 운영진 조회 가능.

#### 응답

```
200 OK
```

| 이름           | 타입        | 설명               |
| -------------- | ----------- | ------------------ |
| `report`       | 객체 배열 | 리포트 리스트
| `report.id`           | id        | `group_activity_report.id` |
| `report.groupId`      | bigint | group.id|
| `report.seminarDate`         | int      | 세미나 일자 |
| `report.seminarSeason`         | int      | 세미나 시즌 summer or winter |
| `report.seminarIsSpeakAfter`   | boolean      | false:먼저말하기 true:나중에말하기|
| `report.isPresentation` | boolean     | 발표 여부          |
| `report.reportFile`        | 문자열 배열 | 보고 파일 url   |
| `report.created_at`        | timestamp | 생성 일자  |
| `report.updated_at`        | timestamp | 변경 일자  |

#### 테스트 케이스

1. 존재하지 않는 `groupId` → 404
2. 그룹원이 아님 → 403
3. 그룹원이 조회 요청 → 200
4. 운영진이 조회 요청 → 200
