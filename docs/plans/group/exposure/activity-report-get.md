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
| `reports[].seminarDate`          | string\|null       | 세미나 일시 (`schedule.scheduledAt`, ISO 8601 UTC, `Z` 접미사). 세미나/스케줄 참조 row 누락 시 null |
| `reports[].seminarIsSummerSeason` | boolean\|null     | true: 여름, false: 겨울. 세미나 참조 row 누락 시 null                                           |
| `reports[].seminarIsSpeakAfter`  | boolean\|null      | false:먼저말하기 true:나중에말하기. 세미나 참조 row 누락 시 null                                |
| `reports[].isPresentation`       | boolean            | 발표 여부                                                                                   |
| `reports[].reportFileUrl`        | string             | 보고 파일 접근 URL. R2 버킷이 public이면 URL, private이면 백엔드가 생성한 Presigned GET URL (클라에서 다운받기 위함이므로 key가 아닌 url) |
| `reports[].createdAt`            | string             | 생성 일시 (ISO 8601 UTC, `Z` 접미사)                                                        |
| `reports[].updatedAt`            | string             | 변경 일시 (ISO 8601 UTC, `Z` 접미사)                                                        |


## 연관 데이터 누락과 장애

세미나/스케줄 참조 row 누락 시 null이라는 응답 규칙은 조회가 정상 완료됐으나 참조 row가 없는 경우를 뜻한다. 세미나가 없으면 날짜·계절·발표 순서 모두 null, 스케줄만 없으면 날짜만 null이다. DB 연결 실패·쿼리 오류는 500으로 응답하며 정상 빈 목록이나 null 메타데이터로 위장하지 않는다.

## 테스트 케이스

1. 그룹원이나 운영진은 그룹의 활동보고 목록을 조회할 수 있다
   - 200 응답과 함께 이 그룹의 활동보고를 반환한다. 그룹원이 아닌 운영진도 조회할 수 있다
   - 보고서 아이디는 문자열, 그룹 아이디는 숫자(Long), 시각은 UTC ISO 8601 `Z` 형식이다. 활동보고가 없으면 `reports: []`다
2. 보고 파일은 내려받을 수 있는 주소로 반환한다
   - 공개 버킷이면 접근 URL, 비공개 버킷이면 Presigned GET URL을 반환하며 파일 key로 대신하지 않는다
3. 세미나 정보가 없으면 해당 필드만 null이다
   - 세미나가 없으면 날짜·계절·발표 순서가 모두 null, 일정만 없으면 날짜만 null이다. 활동보고는 그대로 반환한다
   - DB 조회 자체가 실패하면 500 에러를 반환한다
4. 그룹 상태와 관계없이 조회할 수 있다
   - 중단·종료 그룹에서도 200 응답을 반환한다
5. 그룹원이나 운영진만 조회할 수 있다
   - 둘 다 아니면 403, 인증되지 않으면 401, 그룹이 없으면 404 에러를 반환한다

## 관련 DB 테이블

| 테이블                   | 역할                                       |
| ------------------------ | ------------------------------------------ |
| `group`                  | 그룹 존재 확인                             |
| `group_member`           | 요청자가 그룹 멤버인지 확인                |
| `group_activity_report`  | 활동보고 조회                              |
| `big_seminar`+`schedule` | 세미나 정보 조회 (`season`, `scheduledAt`) |
