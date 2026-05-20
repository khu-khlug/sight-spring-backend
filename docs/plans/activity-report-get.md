## 활동보고 조회 API

### 비즈니스 규칙

- 그룹 멤버 누구나 조회 가능
- 운영진 조회 가능

### 관련 DB 테이블

| 테이블                   | 역할                                       |
| ------------------------ | ------------------------------------------ |
| `group`                  | 그룹 존재 확인                             |
| `group_member`           | 요청자가 그룹 멤버인지 확인                |
| `group_activity_report`  | 활동보고 조회                              |
| `big_seminar`+`schedule` | 세미나 정보 조회 (`season`, `scheduledAt`) |

### API

```
GET /groups/:groupId/activity-report
```

#### 응답

```
200 OK
```

| 이름                         | 타입      | 설명                                                                                        |
| ---------------------------- | --------- | ------------------------------------------------------------------------------------------- |
| `reports`                        | 객체 배열          | 리포트 리스트                                                                               |
| `reports[].id`                   | id                 | `group_activity_report.id`                                                                  |
| `reports[].groupId`              | bigint             | group.id                                                                                    |
| `reports[].seminarDate`          | timestamp\|null    | 세미나 일자 (`schedule.scheduledAt`). 세미나/스케줄 조회 실패 시 null                       |
| `reports[].seminarIsSummerSeason` | boolean\|null     | true: 여름, false: 겨울. 세미나 조회 실패 시 null                                           |
| `reports[].seminarIsSpeakAfter`  | boolean\|null      | false:먼저말하기 true:나중에말하기. 세미나 조회 실패 시 null                                |
| `reports[].isPresentation`       | boolean            | 발표 여부                                                                                   |
| `reports[].reportFileUrl`        | string             | 보고 파일 접근 URL. R2 버킷이 public이면 URL, private이면 백엔드가 생성한 Presigned GET URL (클라에서 다운받기 위함이므로 key가 아닌 url) |
| `reports[].created_at`           | timestamp          | 생성 일자                                                                                   |
| `reports[].updated_at`           | timestamp          | 변경 일자                                                                                   |

#### 테스트 케이스

1. 존재하지 않는 `groupId` → 404
2. 그룹원이 아님 → 403
3. 그룹원이 조회 요청 → 200
4. 운영진이 조회 요청 → 200
