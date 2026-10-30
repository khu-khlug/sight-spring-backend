# 그룹 상세 — 변경이 필요한 기존 API

현재 작업 트리의 스프링 구현을 `docs/plans/group/` 명세와 비교하여, 현재 동작과 변경할 동작을 정리한다. `origin/main`이나 레거시를 현재 구현으로 간주하지 않는다. 새로 구현할 API는 [new-apis.md](new-apis.md)에 정리한다.

대상은 그룹 상세 명세에 대응하는 기존 API다. 그룹 검색 목록·그룹 매칭·활동 일정·내부 디스코드 조회 API에는 이 문서를 근거로 별도 변경을 적용하지 않는다. 경로의 `{groupId}`와 명세의 `:groupId`는 같은 경로 변수 표기다.

## 구현 근거와 공통 기준

- 즐겨찾기·포트폴리오: [GroupController](../../../src/main/kotlin/com/sight/controllers/http/GroupController.kt), [GroupService](../../../src/main/kotlin/com/sight/service/GroupService.kt).
- 채팅방 생성·참여: 위 컨트롤러와 [GroupDiscordChannelService](../../../src/main/kotlin/com/sight/service/GroupDiscordChannelService.kt).
- 멤버 조회·위임·참가·내보내기·나가기: [GroupMemberController](../../../src/main/kotlin/com/sight/controllers/http/GroupMemberController.kt), [GroupMemberService](../../../src/main/kotlin/com/sight/service/GroupMemberService.kt), [GroupMemberRepository](../../../src/main/kotlin/com/sight/repository/GroupMemberRepository.kt).
- 활동보고: [GroupActivityReportController](../../../src/main/kotlin/com/sight/controllers/http/GroupActivityReportController.kt), [GroupActivityReportService](../../../src/main/kotlin/com/sight/service/GroupActivityReportService.kt), [FileUploadService](../../../src/main/kotlin/com/sight/service/FileUploadService.kt).
- 아이디 타입은 [공통 기준](../../plans/group/global.md)에 따라 현재 도메인 선언을 따른다. 그룹·회원·그룹 로그의 `Long`은 유지한다. 활동보고·세미나·파일 업로드 아이디는 현재처럼 문자열이다.
- 이미 구현된 경험치 수치는 변경 사항으로 취급하지 않는다. [경험치 기준](../../plans/experience-points.md)에 따라 참가 +40, 내보내기·나가기 -40, 포트폴리오 ±10, 보고서·세미나 발표 제출·취소 ±150은 유지한다. 실습 그룹의 경험치 제외 예외는 제거한다.

## 즐겨찾기·참가·채팅방

### 그룹 즐겨찾기 추가 — [명세](../../plans/group/info/group-bookmark-add.md)

**현재 구현**

- `POST /groups/{groupId}/bookmark`, 성공 `201`, 응답 바디 없음.
- 그룹 존재 여부와 중복 여부만 검사한다. 중복은 `422`, 그룹 미존재는 `404`다.
- 즐겨찾기를 저장하며 알림은 발송하지 않는다.

**변경할 내용**

- 저장 전에 그룹 열람 권한을 검사한다. 권한이 없으면 `403`을 반환한다.
- 성공 시 요청자에게 즐겨찾기 추가 알림을 발송한다.
- 기존 경로와 `201`·`422`·`404`는 유지한다.

### 그룹 즐겨찾기 취소 — [명세](../../plans/group/info/group-bookmark-remove.md)

**현재 구현**

- `DELETE /groups/{groupId}/bookmark`, 성공 `204`.
- 그룹 존재 여부와 즐겨찾기 여부만 검사한다. 미등록은 `422`, 그룹 미존재는 `404`다.
- 즐겨찾기를 삭제하며 알림은 발송하지 않는다.

**변경할 내용**

- 삭제 전에 그룹 열람 권한을 검사한다. 권한이 없으면 `403`을 반환한다.
- 성공 시 요청자에게 즐겨찾기 해제 알림을 발송한다.
- 기존 경로와 `204`·`422`·`404`는 유지한다.

### 그룹 참가 — [명세](../../plans/group/info/group-join.md)

**현재 구현**

- `POST /groups/{groupId}/members/@me`, 요청 바디 없음, 성공 `201`.
- 열람 권한 없음·참가 차단은 `403`, 종료 상태·이미 참가한 회원은 `400`, 그룹 미존재는 `404`다.
- 멤버 추가, 인원 수 증가, 참가 로그, 본인·그룹장 알림이 구현돼 있다. 운영 그룹은 그룹장 대신 전체 멤버에게 알린다.
- `changedAt` 갱신과 중단 그룹의 자동 재진행도 구현돼 있다.
- 참가자에게 +40 exp를 지급하되 `GroupPolicy.EXPOINT_EXCLUDED_GROUP_ID`인 그룹은 제외한다.

**변경할 내용**

- 실습 그룹 아이디에 따른 경험치 지급 제외 조건을 제거하여 참가자에게 +40 exp를 적용한다.
- 경로·요청·응답 코드와 나머지 기존 동작은 유지한다. 로그 형식 변경은 별도 로그 Task를 따른다.

### 그룹 채팅방 생성 — [명세](../../plans/group/info/group-chat-room-create.md)

**현재 구현**

- `POST /groups/{groupId}/discord-channel`, 성공 `200`과 `{ id, groupId, discordChannelId, createdAt }`를 반환한다.
- 그룹장 여부를 확인하고 비공개 디스코드 채널 생성 후 요청자를 참여시킨다.
- 채팅방 중복과 디스코드 미연동은 모두 `422`다.

**변경할 내용**

- 경로를 `POST /groups/{groupId}/chat-room`으로 변경한다.
- 성공 응답을 `204 No Content`로 변경하고 응답 DTO를 제거한다.
- 채팅방 중복은 `422`에서 `400`으로 변경한다. 미연동 `422`와 그룹장 제한은 유지한다.
- 알림·활동 로그는 명세에서 미정이므로 임의로 추가하지 않는다.

### 그룹 채팅방 참여 — [명세](../../plans/group/info/group-chat-room-join.md)

**현재 구현**

- `POST /groups/{groupId}/discord-channel/members`, 바디 `{ memberId }`, 성공 `204`.
- 그룹장은 다른 그룹원을 추가할 수 있고 일반 그룹원은 자신만 추가할 수 있다.
- 채팅방 미존재는 `404`, 디스코드 미연동은 `422`다. 이미 참여한 경우를 별도로 검사하지 않는다.

**변경할 내용**

- 경로를 `POST /groups/{groupId}/chat-room/join`으로 변경하고 요청 바디를 제거한다.
- 인증된 요청자 아이디를 참여 대상으로 사용하고 요청자의 그룹원 여부를 검사한다. 그룹장의 타인 추가 기능을 제거한다.
- 그룹은 존재하지만 채팅방이 없으면 `400`을 반환한다. 그룹 자체 미존재 `404`와 미연동 `422`는 유지한다.
- 디스코드 참여 여부를 확인하여 이미 참여했으면 `400`을 반환한다. 기존 `checkUserInDiscordChannel`의 확인 로직을 활용할 수 있다.
- 성공 `204`는 유지한다. 알림·활동 로그는 명세의 미정 사항이다.

## 그룹 멤버

### 그룹 멤버 목록 조회 — [명세](../../plans/group/member/group-members-get.md)

**현재 구현**

- `GET /groups/{groupId}/members`, 성공 `200`, 열람 권한 검사 있음.
- 응답은 `{ members: [{ userId, name, realname, isLeader }] }`다. `name`과 실명 `realname`은 별도 값이다.
- 이미 그룹장이 맨 앞에 오며, 나머지는 회원 아이디 오름차순이다.
- 조회 SQL은 회원 아이디·이름·실명만 읽는다. 현재 `GroupMember` 도메인에는 그룹·회원 아이디만 있다.

**변경할 내용**

- 최상위 `leaderUserId`를 추가하고 멤버별 `isLeader`·`realname`을 제거한다.
- `members[].name`에는 기존 `realname` 값을 반환한다.
- `college`, 삭제되지 않은 본인 작성 카드 수 `cardCount`, 삭제되지 않은 본인 작성 기록 수 `recordCount`를 추가한다. 현재 카드·기록 도메인이 없으므로 해당 기능 구현과 연계해야 한다.
- 그룹장 우선 정렬은 유지하고, 나머지는 회원 아이디순에서 회원 실명(`members[].name`, 현재 회원 도메인의 `realname`) 오름차순으로 변경한다. 백엔드에서 정렬을 완료한 목록을 반환한다.
- 경로와 성공 `200`, 기존 열람 권한 검사는 유지한다.

### 그룹장 위임 — [명세](../../plans/group/member/group-leader-delegate.md)

**현재 구현**

- `PATCH /groups/{groupId}/master`, 바디 `{ memberId }`, 성공 `204`.
- 그룹장 권한·본인 위임 금지·대상의 그룹원 여부를 검사한다.
- 그룹장 변경, 로그, 전체 멤버 알림, `changedAt` 갱신과 중단 그룹 자동 재진행이 구현돼 있다.

**변경할 내용**

- 경로를 `PUT /groups/{groupId}/leader`로 변경한다.
- 요청 필드를 `memberId`에서 `userId`로 변경한다. 타입은 `Long`을 유지한다.
- 기존 성공·실패 코드와 부수 효과는 유지한다. 로그 형식 변경은 별도 로그 Task를 따른다.

### 그룹 멤버 내보내기 — [명세](../../plans/group/member/group-member-kick.md)

**현재 구현**

- `DELETE /groups/{groupId}/members/{memberId}`, 성공 `204`.
- 비운영 그룹에서는 그룹장만 요청할 수 있다. 현재 조건은 `category != MANAGE && master != requesterId`이므로 운영 그룹에서는 그룹장 권한 검사까지 건너뛴다.
- 자기 자신·그룹원이 아닌 대상은 `400`, 존재하지 않는 그룹·회원은 `404`다.
- 멤버 삭제, 인원 수 감소, 로그, 대상 알림, `changedAt` 갱신과 중단 그룹 자동 재진행이 구현돼 있다. -40 exp는 실습 그룹에 한해 제외한다.

**변경할 내용**

- 운영(`MANAGE`) 그룹이면 내보내기를 실행하지 않고 `400`을 반환하는 검사를 추가한다. 현재의 운영 그룹 권한 검사 우회 조건을 제거한다.
- 실습 그룹 아이디에 따른 경험치 차감 제외 조건을 제거하여 대상에게 -40 exp를 적용한다.
- 기존 경로와 성공 `204`, 비운영 그룹의 그룹장 제한 `403`, 자기 자신·비멤버 `400`, 그룹·회원 미존재 `404`는 유지한다.
- 나머지 기존 부수 효과는 유지한다. 로그 형식 변경은 별도 로그 Task를 따른다.

### 그룹 나가기 — [명세](../../plans/group/setting/group-leave.md)

**현재 구현**

- `DELETE /groups/{groupId}/members/@me`, 성공 `204`.
- 그룹원 아님·비운영 그룹장의 위임 없는 탈퇴는 `400`, 그룹 미존재는 `404`다. 운영 그룹장은 위임 없이 나갈 수 있다.
- 멤버 삭제, 인원 수 감소, 로그, 본인·그룹장 알림, `changedAt` 갱신과 중단 그룹 자동 재진행이 구현돼 있다. 운영 그룹은 그룹장 대신 남은 멤버 전체에게 알린다.
- -40 exp는 실습 그룹에 한해 제외한다.

**변경할 내용**

- 실습 그룹 아이디에 따른 경험치 차감 제외 조건을 제거하여 탈퇴자에게 -40 exp를 적용한다.
- 기존 경로·응답 코드·부수 효과와 운영 그룹장의 탈퇴 허용은 유지한다. 탈퇴 후 운영 그룹장 처리 정책은 별도 확정 전까지 임의로 바꾸지 않는다.
- 로그 형식 변경은 별도 로그 Task를 따른다.

## 그룹 활동 로그

[로그 API 명세](../../plans/group/history/group-logs-get.md)와 [현재 구현에서 로그타입·로그데이터로 변경하는 Task](group-log-format.md)를 따른다. 저장 구조·응답·기존 로깅 호출부의 변경 사항은 해당 Task에만 관리한다.

## 활동보고·포트폴리오

### 활동보고 조회 — [명세](../../plans/group/exposure/activity-report-get.md)

**현재 구현**

- `GET /groups/{groupId}/activity-report`, 성공 `200`. 그룹원 또는 운영진만 조회할 수 있다.
- 목록의 세미나 정보와 파일 다운로드 URL을 조합한다. 일시 필드는 `LocalDateTime`이다.
- 다운로드 URL 생성은 현재 `StubStorageService`를 사용하여 빈 문자열을 반환한다.

**변경할 내용**

- 경로를 `GET /groups/{groupId}/activity-reports`로 변경한다.
- `seminarDate`, `createdAt`, `updatedAt`을 ISO 8601 UTC(`Z` 접미사)로 반환한다. 기존 DB 시각의 기준 시간대를 확인한 뒤 변환하며, 시간대 없는 값에 `Z`만 붙이지 않는다.
- 실제 스토리지 구현을 연결하여 접근 가능한 파일 URL을 반환한다.
- 권한과 성공 `200`, 나머지 응답 필드는 유지한다. 현재 명세에도 없는 `seminarId`·`fileName` 추가를 확정된 변경 사항으로 취급하지 않는다.

### 활동보고 파일 업로드 링크 발급 — [명세](../../plans/group/exposure/activity-report-upload-link.md)

**현재 구현**

- `POST /groups/{groupId}/activity-report/upload-link`, 성공 `200`, `{ url, fileKey, fileUploadId }`를 반환한다. 그룹장 제한이 있다.
- `uploadLinkRequestPath(groupId)`는 실제 그룹 아이디가 들어간 경로를 `file_upload.api_path`에 저장한다.
- `StubStorageService`는 업로드 URL을 빈 문자열로 반환하며 실제 업로드·20 MiB 제한을 구현하지 않는다. `FileUpload`의 사용 여부 필드는 `isUsed`/`is_used`다.

**변경할 내용**

- 경로와 `uploadLinkRequestPath(groupId)`를 `/groups/{groupId}/activity-reports/upload-link`로 변경한다. 제출·수정에서 검증하는 값도 동일한 실제 경로를 사용한다.
- 실제 스토리지 URL 발급·만료 처리와 명세의 20 MiB(20 × 1024 × 1024바이트) 업로드 제한을 구현한다. 현재 인터페이스에는 크기 제한 인자가 없으므로 제한을 강제할 수 있도록 구현을 보완해야 한다.
- 배포 전 발급한 미사용 링크는 이전 `api_path`를 가진다. 경로 변경 시 해당 링크의 처리 방식을 정해야 한다.
- 성공 코드·응답 필드·그룹장 권한은 유지한다. 명세의 `isVerified`는 현재 구현명 `isUsed`와 다르므로 문서 용어를 정합화하며, 필드 이름만을 근거로 DB 컬럼을 변경하지 않는다.

### 활동보고 제출 — [명세](../../plans/group/exposure/activity-report-submit.md)

**현재 구현**

- `POST /groups/{groupId}/activity-report`, 성공 `201`. 접수 가능한 가장 가까운 세미나를 찾고 중복 제출을 검사한다.
- 그룹장 제한, 업로드 검증·사용 처리, 보고 저장, 멤버 전체 +150 exp, 멤버·운영진 알림이 구현돼 있다. 발표 여부와 관계없이 +150 exp다.
- 그룹 활동 로그와 `group.changedAt` 갱신은 없다. 응답 일시는 `LocalDateTime`이다.

**변경할 내용**

- 경로를 `POST /groups/{groupId}/activity-reports`로 변경하고 업로드 검증 경로도 새 경로로 변경한다.
- 아래 공통 파일 검증 차이를 반영한다.
- 제출 로그와 그룹 최근 활동 일시 갱신을 추가한다. 활동보고 이벤트의 로그타입·데이터 스키마는 별도 로그 Task와 맞춰 확정해야 한다.
- `createdAt`·`updatedAt`을 기준 시간대에 맞춰 UTC(`Z`)로 반환한다.
- 요청 필드·성공 `201`·기존 제출 조건·+150 exp·알림은 유지한다.

### 활동보고 수정 — [명세](../../plans/group/exposure/activity-report-edit.md)

**현재 구현**

- `PATCH /groups/{groupId}/activity-report/{reportId}`, 성공 `200`.
- 그룹장 제한, 보고의 그룹 소속·접수 기간 검사, 수정값 하나 이상 필수, 파일 교체·이전 파일 삭제, 변경 종류별 멤버·운영진 알림이 구현돼 있다. 경험치는 변하지 않는다.
- 그룹 활동 로그와 `group.changedAt` 갱신은 없다. 응답 일시는 `LocalDateTime`이다.

**변경할 내용**

- 경로를 `PATCH /groups/{groupId}/activity-reports/{reportId}`로 변경하고 업로드 검증 경로도 새 경로로 변경한다.
- 파일 교체 시 아래 공통 파일 검증 차이를 반영한다.
- 수정 로그와 그룹 최근 활동 일시 갱신을 추가한다. 활동보고 이벤트의 로그타입·데이터 스키마는 별도 로그 Task와 맞춰 확정해야 한다.
- `createdAt`·`updatedAt`을 기준 시간대에 맞춰 UTC(`Z`)로 반환한다.
- 요청 필드·성공 `200`·기존 수정 조건·경험치 변동 없음·알림은 유지한다.

### 제출·수정의 공통 파일 검증 차이

현재 `FileUploadService.validateFileUpload`와 활동보고 명세에는 다음 차이가 있다.

| 상황 | 현재 스프링 구현 | 명세에 맞출 동작 |
| :-- | :-- | :-- |
| 업로드 아이디에 해당하는 행 없음 | `404` | `400` |
| 발급 API 경로 불일치 | `422` | `400` |
| 발급자 불일치 | `403` | `400` |
| 이미 사용된 업로드 | `422` | `400` |
| 스토리지에 파일 없음 | `404`, 업로드 행 유지 | 업로드 행 삭제 후 `400` |

이 차이는 활동보고 제출·수정 계약에 적용한다. 공용 서비스를 수정할 때는 다른 사용처의 계약까지 일괄 변경하지 않도록 영향 범위를 확인한다. 파일 미존재 시 행 삭제는 예외로 인한 트랜잭션 롤백 후에도 삭제가 유지되는 처리 방식이 필요하다. 실제 존재 검사는 현재 항상 `true`를 반환하는 스토리지 스텁을 교체해야 동작한다.

### 활동보고 취소 — [명세](../../plans/group/exposure/activity-report-cancel.md)

**현재 구현**

- `DELETE /groups/{groupId}/activity-report/{reportId}`, 성공 `204`.
- 그룹장 또는 운영진 권한, 보고의 그룹 소속과 접수 기간을 검사한다.
- 보고·업로드 행·스토리지 파일 삭제, 멤버 전체 -150 exp, 멤버·운영진 알림이 구현돼 있다. 발표 여부와 관계없이 -150 exp다. 실제 파일 삭제는 현재 스토리지 스텁에서 아무 작업도 하지 않는다.
- 그룹 활동 로그와 `group.changedAt` 갱신은 없다.

**변경할 내용**

- 경로를 `DELETE /groups/{groupId}/activity-reports/{reportId}`로 변경한다.
- 취소 로그와 그룹 최근 활동 일시 갱신을 추가한다. 활동보고 이벤트의 로그타입·데이터 스키마는 별도 로그 Task와 맞춰 확정해야 한다.
- 실제 스토리지 파일 삭제를 연결한다.
- 성공 `204`·기존 취소 조건·-150 exp·알림은 유지한다.
- 운영진의 세미나 세션 강제 제거에도 -150 exp를 적용한다는 결정은 유지한다. 이 접수 중 활동보고 취소 API가 강제 제거 전체를 이미 구현한 것으로 간주하거나, 해당 결정을 근거로 기존 접수 기간 검사를 제거하지 않는다.

### 포트폴리오 발행 — [명세](../../plans/group/exposure/group-portfolio-publish.md)

**현재 구현**

- `POST /groups/{groupId}/portfolio`, 성공 `200`, `{ published: true }`.
- 그룹장 제한, 이미 발행했으면 `400`, 그룹 미존재 `404`가 구현돼 있다.
- 발행 상태 저장, 멤버 전체 +10 exp와 알림은 있지만 그룹 활동 로그는 없다.

**변경할 내용**

- 발행 성공 시 그룹 활동 로그를 추가한다. 로그 형식은 별도 로그 Task를 따른다.
- 경로·응답·권한·실패 코드·+10 exp·알림은 유지한다.

### 포트폴리오 취소 — [명세](../../plans/group/exposure/group-portfolio-cancel.md)

**현재 구현**

- `DELETE /groups/{groupId}/portfolio`, 성공 `204`.
- 그룹장 제한, 미발행이면 `404`, 그룹 미존재 `404`가 구현돼 있다.
- 발행 상태 해제, 멤버 전체 -10 exp와 알림은 있지만 그룹 활동 로그는 없다.

**변경할 내용**

- 취소 성공 시 그룹 활동 로그를 추가한다. 로그 형식은 별도 로그 Task를 따른다.
- 경로·응답·권한·실패 코드·-10 exp·알림은 유지한다.
