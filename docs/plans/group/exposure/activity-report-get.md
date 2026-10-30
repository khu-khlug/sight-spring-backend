# 활동보고 조회

그룹이 제출한 활동보고 목록을 조회합니다.

## 운영 그룹인 경우

(해당 없음)

## API 경로

```
GET /groups/:groupId/activity-reports
```

## 권한

USER and (그룹원 or 운영진)

## 조건

(해당 없음)

## 부수 효과

(해당 없음)

## 쿼리스트링

(해당 없음)

## 요청 바디

(해당 없음)

## 성공 응답

```
200 OK
```

| 이름                         | 타입      | 설명                                                                                        |
| ---------------------------- | --------- | ------------------------------------------------------------------------------------------- |
| `reports`                        | 객체 배열          | 리포트 리스트                                                                               |
| `reports[].id`                   | 문자열 | `group_activity_report.id`                                                                  |
| `reports[].groupId`              | 숫자(Long) | group.id                                                                                    |
| `reports[].seminarDate`          | string\|null       | 세미나 일시 (`schedule.scheduledAt`, ISO 8601 UTC, `Z` 접미사). 세미나/스케줄 조회 실패 시 null |
| `reports[].seminarIsSummerSeason` | boolean\|null     | true: 여름, false: 겨울. 세미나 조회 실패 시 null                                           |
| `reports[].seminarIsSpeakAfter`  | boolean\|null      | false:먼저말하기 true:나중에말하기. 세미나 조회 실패 시 null                                |
| `reports[].isPresentation`       | boolean            | 발표 여부                                                                                   |
| `reports[].reportFileUrl`        | string             | 보고 파일 접근 URL. R2 버킷이 public이면 URL, private이면 백엔드가 생성한 Presigned GET URL (클라에서 다운받기 위함이므로 key가 아닌 url) |
| `reports[].createdAt`            | string             | 생성 일시 (ISO 8601 UTC, `Z` 접미사)                                                        |
| `reports[].updatedAt`            | string             | 변경 일시 (ISO 8601 UTC, `Z` 접미사)                                                        |


## 테스트 케이스

1. 존재하지 않는 `groupId` → 404
2. 그룹원이 아님 → 403
3. 그룹원이 조회 요청 → 200
4. 운영진이 조회 요청 → 200

## 관련 DB 테이블

| 테이블                   | 역할                                       |
| ------------------------ | ------------------------------------------ |
| `group`                  | 그룹 존재 확인                             |
| `group_member`           | 요청자가 그룹 멤버인지 확인                |
| `group_activity_report`  | 활동보고 조회                              |
| `big_seminar`+`schedule` | 세미나 정보 조회 (`season`, `scheduledAt`) |
