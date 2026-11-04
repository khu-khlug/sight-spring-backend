# 그룹 권한 어노테이션

`@Auth`는 사용자 역할을, `@GroupAuth`는 사용자 역할과 그룹·기록 권한을 검사한다. API에는 둘 중 하나만 선언한다.

## 기본 사용자 범위

기존 `Auth`와 `UserRole`의 의미·선언·인증 결과는 유지한다.

| UserRole | 허용 대상 |
| :-- | :-- |
| USER | 일반 회원만 |
| MANAGER | 운영진만 |
| SYSTEM | 시스템 호출만 |

`@GroupAuth`는 기본적으로 `@Auth([UserRole.USER, UserRole.MANAGER])`가 적용된 것으로 한다. 일반 회원과 운영진을 대상으로 그룹 권한을 추가 검사하며 `UserRole`을 인자로 받지 않는다.

## 그룹 권한

| GroupRole | 허용 조건 |
| :-- | :-- |
| VIEWER | MEMBER를 만족하거나 그룹 공개 범위에 따라 열람 가능 |
| MEMBER | 해당 그룹의 그룹원. LEADER·WRITER를 만족하는 요청자도 포함 |
| LEADER | 해당 그룹의 `leaderMemberId`와 요청자 아이디가 일치함. 그룹장이 null이면 누구도 만족하지 않음 |
| WRITER | 해당 그룹의 그룹원이면서 해당 기록의 작성자. 기록도 해당 그룹 소속이어야 함 |
| MANAGER | 운영진이면서 해당 그룹의 `category == GroupCategory.MANAGE` |

포함 관계는 `VIEWER ⊇ MEMBER ⊇ (LEADER ∪ WRITER)`다. LEADER는 그룹장 여부로, WRITER는 해당 기록의 작성자 여부로 독립적으로 판정한다. 그룹장은 본인 기록에서 LEADER와 WRITER를 모두 만족하고, 다른 사람의 기록에서는 LEADER만 만족한다. GroupRole.MANAGER는 별도 조건이다.

그룹원은 공개 범위와 관계없이 VIEWER를 만족한다. 비그룹원은 아래 공개 범위로 판정하며, 비공개 그룹의 생성자도 그룹원이 아니면 거부한다. 모든 VIEWER권한인 API들에 같은 규칙을 적용한다.

| 공개 범위 | 비그룹원 중 허용 대상 |
| :-- | :-- |
| 비공개 | 없음 |
| 운영진 공개 | 운영진 |
| 회원 공개 | 교류 회원을 제외한 회원 |
| 완전 공개 | 모든 로그인 회원 |

## 선언 형식

```kotlin
annotation class GroupAuth(
    val roles: Array<GroupRole>,
    val groupId: String = "groupId",
    val recordId: String = "recordId",
    val allowManager: Boolean = false,
)
```

- roles 배열의 그룹 권한은 OR로 결합하며, 모두 같은 groupId·recordId를 기준으로 판정한다.
- groupId·recordId는 실제 아이디가 아닌 Spring 경로 변수 이름이다. 기본 이름과 같으면 생략할 수 있다. WRITER에만 recordId가 필요하며 기록의 현재 그룹 소속을 검사한다.
- allowManager가 true이면 운영진은 그룹 소속·유형과 관계없이 허용한다. 기본값은 false이며, 운영 그룹만 허용하는 GroupRole.MANAGER와 구분한다.
- 다른 사용자 역할 조건은 필요할 때 확장한다.
- 빈 roles 배열·필요한 경로 변수 이름 누락·Auth와 GroupAuth 동시 선언은 선언 오류다.

```kotlin
@GroupAuth([GroupRole.WRITER])
@PatchMapping("/groups/{groupId}/records/{recordId}")
fun updateRecord(...) { ... }

@GroupAuth(
    [GroupRole.LEADER, GroupRole.MANAGER],
    groupId = "targetGroupId",
)
fun updateGroup(...) { ... }
```

## 조합별 선언

아래 선언의 아이디 이름은 기본값을 사용한다.

| 허용 조건 | 선언 |
| :-- | :-- |
| 일반 회원 또는 운영진 | `@Auth([UserRole.USER, UserRole.MANAGER])` |
| 그룹 열람권한 | `@GroupAuth([GroupRole.VIEWER])` |
| 그룹원 | `@GroupAuth([GroupRole.MEMBER])` |
| 그룹장 | `@GroupAuth([GroupRole.LEADER])` |
| 그룹원이면서 기록 작성자 | `@GroupAuth([GroupRole.WRITER])` |
| 그룹원 또는 운영진 | `@GroupAuth([GroupRole.MEMBER], allowManager = true)` |
| 그룹장 또는 운영진 | `@GroupAuth([GroupRole.LEADER], allowManager = true)` |
| 그룹장 또는 운영 그룹의 운영진 | `@GroupAuth([GroupRole.LEADER, GroupRole.MANAGER])` |
| 운영진 | `@Auth([UserRole.MANAGER])` |
| 시스템 | `@Auth([UserRole.SYSTEM])` |

## 판정과 실패 처리

```text
Auth 허용 = 선언한 UserRole 중 하나를 만족
GroupAuth 허용 = (UserRole.USER OR UserRole.MANAGER)
                AND (roles 중 하나를 만족 OR (allowManager AND 운영진))
```

- 인증되지 않았으면 401, 권한을 만족하지 못하면 403이다. GroupAuth에 SYSTEM으로 접근하면 403이다.
- 그룹 판정 대상이 없고 성공한 다른 그룹 권한이나 운영진 예외도 없으면 404다. 다른 그룹의 기록으로 WRITER 권한을 얻을 수 없다.
- 한 그룹 권한이나 운영진 예외가 성공하면 다른 분기의 권한 실패·대상 없음이 허용을 막지 않는다. 허용 후에도 서비스의 대상 존재·소속·삭제 상태 검사는 유지한다.
- 아이디 형식 오류는 400, 어노테이션 선언 오류는 500이며 비즈니스 동작을 실행하지 않는다.
- 어노테이션이 없는 공개 API의 계약은 유지한다.

## 적용 범위와 순서

1. 기존 Auth·사용자 역할·인증 결과를 유지하고 GroupAuth를 추가한다. 그룹 도메인의 판정은 그룹 기능에 둔다.
2. 그룹 권한 배열의 OR 결합·운영진 예외·중복 선언 검사를 구현한다.
3. 모든 VIEWER API에 공개 범위·포함 관계를 적용하고 비공개 생성자 예외를 제거한다.
4. 서비스의 그룹 권한 검사를 제거하는 API는 같은 변경에서 Auth를 GroupAuth로 교체한다.
5. [기존 API 변경](../tasks/group/api-changes.md)과 [신규 API](../tasks/group/new-apis.md)에도 같은 선언을 적용한다. 채팅방 참여는 본인 참여 API로 변경한 뒤 MEMBER로 옮긴다.

그룹 상태·참가 허용·접수 기간·삭제 여부·파일 소유권·본인 데이터 제한은 서비스에 둔다. WRITER는 기록 도메인 구현과 연계하며 판정이 준비되기 전에 기존 검사를 제거하지 않는다. 특수 조직 제한의 데이터 구조와 판정 추가는 별도 작업이다.
