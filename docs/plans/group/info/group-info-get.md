# 그룹 정보 조회

그룹 정보를 조회합니다. 요청자 기준의 상태(그룹원 여부, 즐겨찾기 여부, 채팅방 참여 여부)도 함께 반환합니다.

## 운영 그룹인 경우

(해당 없음)

## API 경로

```
GET /groups/:groupId/info
```

## 권한

USER and 열람권한

- 열람권한: 그룹의 공개 범위와 특수 조직 제한을 통과해야 함

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

| 이름                              |      타입       | 설명                                                                                                   |
| :-------------------------------- | :-------------: | :----------------------------------------------------------------------------------------------------- |
| `id`                              | 숫자(Long) | 그룹 아이디                                                                                            |
| `title`                           |     문자열      | 그룹 이름, not null                                                                                    |
| `description`                     | 문자열 \| null  | 그룹 설명                                                                                              |
| `category`                        |      enum       | 그룹 유형. `PROJECT`(프로젝트) \| `STUDY`(스터디) \| `DOCUMENTATION`(문서화) \| `MANAGE`(운영) \| `EDUCATION`(교육) \| `PROGRAM`(프로그램) |
| `status`                          |      enum       | 진행 상태. `PROGRESS`(진행중) \| `STOP`(중단) \| `SUCCESS`(종료(성공)) \| `FAIL`(종료(실패))           |
| `requesterInfo`                   |      객체       | 요청자 기준 정보                                                                                       |
| `requesterInfo.userRole`          |      enum       | 요청자의 그룹원 여부. `NONE`(아님) \| `MEMBER`(그룹원) \| `LEADER`(그룹장)                             |
| `requesterInfo.isBookmarked`      |     불리언      | 요청자의 즐겨찾기 여부                                                                                 |
| `requesterInfo.isInChatRoom`      |     불리언      | 요청자의 채팅방 참여 여부. 채팅방이 없거나 그룹원이 아니면 false |
| `specialOrganization`             |   enum \| null  | 특수 조직. (보안해킹트랙 등). 이번 구현에선 관련 테이블을 만들지 않으므로 컨트롤러에서 항상 null로 설정 |
| `interests`                       | enum 배열 \| null | 관심 분야 배열. `WEB_APP_SERVICE` \| `UX_UI_DESIGN` \| `GAME_GRAPHICS` \| `AI_DATA_SCIENCE` \| `CIRCUIT_IOT` \| `NETWORK_CLOUD` \| `SYSTEM_PROGRAMMING` \| `SECURITY_HACKING` \| `ALGORITHM_THEORY` |
| `techStack`                       | 문자열 배열 \| null | 사용 기술 목록                                                                                       |
| `repositoryUrls`                  | 문자열 배열 \| null | 저장소 URL 목록                                                                                     |
| `memberCount`                     |   숫자(uint)    | 멤버 수                                                                                                |
| `activity`                        |      객체       | 활동 기록                                                                                              |
| `activity.listCount`              |   숫자(uint)    | 리스트 수                                                                                              |
| `activity.cardCount`              |   숫자(uint)    | 카드 수                                                                                                |
| `activity.recordCount`            |   숫자(uint)    | 기록 수                                                                                                |
| `createdAt`                       |     문자열      | 생성 일시 (ISO 8601 UTC, `Z` 접미사)                                                                   |
| `lastActivityAt`                  |     문자열      | 최근 활동 일시 (ISO 8601 UTC, `Z` 접미사)                                                              |
| `allowJoin`                       |     불리언      | 참여 신청 가능 여부                                                                                    |
| `visibility`                      |      enum       | 공개 범위. `PRIVATE`(그룹원만) \| `MEMBER_ONLY`(쿠러그 회원) \| `PUBLIC`(전체) \| `MANAGER_ONLY`(운영진만) |
| `exposure`                        |      객체       | 활동 공개                                                                                              |
| `exposure.portfolioPublished`     |     불리언      | 포트폴리오 발행 여부 |
| `exposure.portfolioListCount`     |   숫자(uint)    | 포트폴리오 리스트 수                                                                                   |
| `exposure.portfolioCardCount`     |   숫자(uint)    | 포트폴리오 카드 수                                                                                     |
| `exposure.seminarCount`           |   숫자(uint)    | 세미나 발표 수                                                                                         |
| `exposure.reportCount`            |   숫자(uint)    | 보고서 제출 수                                                                                         |
| `hasChatRoom`                     |     불리언      | 그룹 채팅방(디스코드 채널) 존재 여부 |
