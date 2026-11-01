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
| `exposure.isPortfolioPublished`   |     불리언      | 포트폴리오 발행 여부 |
| `exposure.portfolioListCount`     |   숫자(uint)    | 포트폴리오 리스트 수                                                                                   |
| `exposure.portfolioCardCount`     |   숫자(uint)    | 포트폴리오 카드 수                                                                                     |
| `exposure.seminarCount`           |   숫자(uint)    | 세미나 발표 수                                                                                         |
| `exposure.reportCount`            |   숫자(uint)    | 보고서 제출 수                                                                                         |
| `hasChatRoom`                     |     불리언      | 그룹 채팅방(디스코드 채널) 존재 여부 |

## 집계 기준

memberCount는 현재 그룹원 수다. 활동 리스트 수는 현재 그룹 리스트, 카드 수는 삭제되지 않은 카드(비활성 포함), 기록 수는 삭제되지 않은 카드의 삭제되지 않은 기록으로 집계한다. 포트폴리오 카드 수는 이 중 isInPortfolio=true인 카드 수이며 포트폴리오 리스트 수는 해당 카드가 하나 이상인 리스트 수다. 그룹 발행 여부와 포함 카운트는 독립적이다. 세미나/보고서 수는 해당 그룹의 활동보고를 isPresentation=true/false로 나눈 수다. 집계 대상이 없으면 0이다. techStack·repositoryUrls는 저장값이 null이면 null, 빈 목록이면 []로 반환한다.

## 테스트 케이스

1. 열람 권한이 있는 회원은 그룹 정보를 조회할 수 있다
   - 200 응답과 함께 그룹 정보·활동 집계·활동 공개·채팅방 존재 여부를 반환한다
   - 그룹 아이디는 숫자(Long), 시각은 UTC ISO 8601 `Z` 형식이다. `specialOrganization`은 항상 null이다
2. 요청자 기준 정보는 요청자별로 반환한다
   - `userRole`은 그룹장 `LEADER`, 그룹원 `MEMBER`, 그 밖에는 `NONE`이다
   - 즐겨찾기·채팅방 참여 여부는 요청자 본인 값이며, 채팅방이 없거나 그룹원이 아니면 `isInChatRoom`은 false다
3. 활동 집계는 삭제되지 않은 항목으로 센다
   - `memberCount`는 현재 그룹원 수, `cardCount`는 비활성을 포함한 삭제되지 않은 카드 수, `recordCount`는 삭제되지 않은 카드의 삭제되지 않은 기록 수다
   - 다른 그룹 데이터는 세지 않고, 대상이 없으면 0이다
4. 포트폴리오 집계는 발행 여부와 따로 센다
   - 포함 카드 2개가 한 리스트에 있으면 `portfolioListCount`는 1, `portfolioCardCount`는 2다. 미발행 그룹도 같고 `isPortfolioPublished`만 false다
   - 발표 보고 2개와 보고서 1개면 `seminarCount`는 2, `reportCount`는 1이다
5. 사용 기술과 저장소 URL은 배열로 반환한다
   - 저장값이 null이면 null, 빈 목록이면 `[]`다. URL 안의 쉼표와 퍼센트 인코딩은 그대로 둔다
6. 그룹 상태와 관계없이 조회할 수 있다
   - 진행·중단·종료 그룹 모두 열람 권한이 있으면 200 응답을 반환한다
7. 공개 범위에 따라 열람할 수 있는 회원이 정해진다
   - 비공개는 그룹원, 운영진 공개는 운영진, 회원 공개는 교류 회원을 제외한 회원, 완전 공개는 모든 로그인 회원이 조회할 수 있다
   - 범위 밖 회원이면 403, 인증되지 않으면 401, 그룹이 없으면 404 에러를 반환한다
8. 레거시 시각도 UTC로 반환한다
   - 레거시 `LocalDateTime`은 저장 시간대로 해석해 같은 시점의 UTC 값으로 반환한다
