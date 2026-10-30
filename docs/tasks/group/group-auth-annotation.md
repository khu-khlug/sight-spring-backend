# 그룹 권한 확인을 컨트롤러 어노테이션으로 옮기기

## 1. 작업 개요

그룹 API마다 서비스 안에서 따로 하고 있는 그룹 권한 확인(열람권한·그룹원·그룹장)을 `@GroupAuth` 어노테이션으로 옮겨 컨트롤러 단에서 확인합니다. 기존 `@Auth`(`core/auth/Auth.kt`, `core/auth/AuthAspect.kt`)와 같은 AOP 방식을 씁니다.

API 문서(`docs/plans/group/`)의 `## 권한`에 적힌 그룹 권한이 어노테이션 하나로 표현되도록 하는 것이 목표입니다.

## 2. 비즈니스 동작

### 권한 수준

| 수준     | 의미                                                                                         |
| :------- | :------------------------------------------------------------------------------------------- |
| `VIEWER` | 열람권한. 그룹 공개 범위를 통과해야 함                                                       |
| `MEMBER` | 요청자가 그룹원이어야 함                                                                     |
| `LEADER` | 요청자가 그룹장(`group.master`)이어야 함                                                     |

열람권한 규칙은 현재 `GroupMemberService.canViewGroup`과 같습니다.

| 공개 범위(`group.grade`) | 열람 가능한 요청자                         |
| :----------------------- | :----------------------------------------- |
| `PRIVATE`                | 그룹원 또는 그룹 생성자(`group.author`)    |
| `MANAGER`                | 운영진                                     |
| `MEMBER`                 | 교류 회원(`StudentStatus.UNITED`)이 아닌 회원 |
| `ALL`                    | 모든 회원                                  |

### 동작

1. `@Auth`가 먼저 로그인 여부와 역할을 확인함 (401·403)
2. `@GroupAuth`가 요청 경로의 `groupId`로 그룹을 찾음 — 없으면 404
3. 요청자가 지정한 수준을 만족하지 못하면 403
4. 통과하면 컨트롤러 메서드를 실행하고, 서비스는 그룹 권한을 다시 확인하지 않음

`allowJoin`, 진행 상태, 채팅방 존재 여부, 접수 기간 같은 상태 조건(`## 조건`)은 권한이 아니므로 서비스에 그대로 둡니다.

### 옮길 대상 (`origin/main` 기준)

| 서비스 · 메서드 | 현재 서비스의 확인 | 어노테이션 |
| :-- | :-- | :-- |
| `GroupMemberService.listGroupMembers` | `canViewGroup` | `VIEWER` |
| `GroupMemberService.joinGroup` | `canViewGroup` | `VIEWER` |
| `GroupMemberService.delegateMaster` | 그룹장 | `LEADER` |
| `GroupMemberService.kickMember` | 그룹장 | `LEADER` |
| `GroupLogService.listGroupLogs` | 그룹원 | `MEMBER` |
| `GroupService.publishPortfolio` | 그룹장 | `LEADER` |
| `GroupService.cancelPortfolio` | 그룹장 | `LEADER` |
| `GroupDiscordChannelService.createDiscordChannel` | 그룹장 | `LEADER` |
| `GroupDiscordChannelService.addMemberToDiscordChannel` | 그룹장은 아무 멤버나, 일반 멤버는 본인만 | 아래 "결정할 점" 참고 |
| `GroupActivityReportService.getUploadLink` | 그룹장 | `LEADER` |
| `GroupActivityReportService.submitReport` | 그룹장 | `LEADER` |
| `GroupActivityReportService.editReport` | 그룹장 | `LEADER` |
| `GroupActivityReportService.cancelReport` | 그룹장 또는 운영진 | `LEADER` + 운영진 허용 |
| `GroupActivityReportService.listReports` | 그룹원 또는 운영진 | `MEMBER` + 운영진 허용 |

[api-changes.md](api-changes.md)·[new-apis.md](new-apis.md)의 API를 구현하거나 바꿀 때도 그룹 권한은 이 어노테이션으로 확인합니다.

## 3. HTTP API 계약

### 변경 사항

- 권한이 없을 때의 응답 코드(403)와 그룹이 없을 때의 응답 코드(404)는 유지함
- 403 응답 메시지는 API마다 다르던 문구("그룹장만 위임할 수 있습니다" 등)에서 어노테이션의 공통 문구로 바뀔 수 있음

### 유지할 계약

- 경로·요청·응답 형식은 바꾸지 않음 (경로 변경은 `api-changes.md`에서 따로 다룸)

## 4. 데이터베이스 계약

변경 없음

## 5. 외부 시스템 계약

변경 없음

## 6. 보안 및 개인정보

- 서비스의 권한 확인을 지우는 API는 반드시 같은 커밋에서 컨트롤러에 `@GroupAuth`를 붙여야 함. 하나라도 빠지면 그 API의 그룹 권한 확인이 사라짐
- 어노테이션이 붙지 않은 그룹 API를 찾을 수 있도록 검증 방법(9절)을 둠

## 7. 비가역적 부작용 및 운영 영향

없음

## 8. 호환성 및 배포

- 요청·응답 형식의 호환성을 유지함
- 403 응답 메시지는 API별 문구에서 공통 문구로 바뀔 수 있음

## 9. 검증

- 옮긴 API마다 권한 수준별로 통과·403·404를 확인하는 테스트
- 운영진 허용 옵션이 있는 API는 운영진 요청이 통과하는지 확인
- `/groups/{groupId}`(또는 `/groups/:groupId`) 경로의 컨트롤러 메서드 중 `@GroupAuth`가 없는 것을 찾는 테스트 또는 lint 규칙

## 10. 비목표

- 상태 조건(`allowJoin`, 진행 상태, 채팅방·접수 기간 등)을 어노테이션으로 옮기지 않음
- 특수 조직 기반 열람 제한은 이번에 구현하지 않음 (관련 테이블을 만들지 않음)
- 경로·요청·응답 변경 (`api-changes.md`)

## 결정할 점

1. **"또는" 조합 표현**
   - "그룹장 또는 운영진", "그룹원 또는 운영진"은 `allowManager` 같은 옵션으로 표현할 수 있음
   - 그룹 정보 변경의 "그룹장 또는 (운영진 and 운영 그룹)"처럼 그룹 유형까지 보는 조합을 어노테이션에 넣을지, 서비스에 남길지
2. **`groupId`를 꺼내는 방법**
   - 메서드 파라미터 이름으로 찾기, 또는 요청의 경로 변수(`HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE`)에서 읽기
3. **Aspect 실행 순서**
   - `@Auth`(401)가 `@GroupAuth`보다 먼저 실행되도록 `@Order` 지정
4. **그룹 중복 조회**
   - Aspect와 서비스가 같은 그룹을 각각 조회함. 같은 요청의 영속성 컨텍스트라면 두 번째 조회는 캐시에서 나오지만, `spring.jpa.open-in-view` 설정은 확인하지 않았음
5. **채팅방 멤버 추가**
   - 현재는 "그룹장은 아무 멤버나, 일반 멤버는 본인만"이라 요청 바디의 `memberId`에 따라 권한이 달라져 어노테이션으로 표현되지 않음
   - `api-changes.md`대로 요청자 본인만 참여하도록 바꾸면 `MEMBER`로 옮길 수 있음
