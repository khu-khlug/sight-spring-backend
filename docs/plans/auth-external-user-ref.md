# 외부 요청 주체와 인증 경계 정리

Discord 이벤트, HTTP 요청, 레거시 인증 서버 등 외부 요청 주체를 내부 유저로 해석하는 책임의 위치를 정리한다.

현재 `core.discord.UserDiscordEventHandler`가 `UserService`에 직접 의존하면서 `core` 계층이 `service` 계층에 의존하는 Konsist 위반이 발생한다. 단순히 의존성만 제거하는 것이 아니라, 외부 요청 주체를 내부 유저로 변환하는 책임을 어디에 둘지 명확히 한다.

## 목표

- `core`는 외부 요청 핸들링과 요청 주체 표현만 담당한다.
- `core`는 내부 유저 조회, Discord 매핑 조회, 레거시 인증 서버 연동을 직접 수행하지 않는다.
- `controllers`는 외부 입력을 받아 기본 전제 조건을 확인하고 service에 필요한 파라미터를 전달한다.
- `service`는 유스케이스에 필요한 시점에 내부 유저 정보를 조회한다.
- `auth` 패키지는 인증/식별 해석에 필요한 읽기 전용 모델과 조회 기능만 제공한다.

## 책임 분리

### core

`core`는 외부 요청을 시스템 내부에서 다루기 쉬운 형태로 변환하는 최소 책임만 가진다.

예상 책임:

- HTTP 요청, Discord 이벤트 등에서 요청 주체를 표현할 수 있는 추상 타입 정의
- 요청 핸들링 과정에서 필요한 최소 전제 조건 확인
  - 세션 값 존재 여부
  - 토큰 존재 여부
  - Discord 이벤트에서 사용자 ID 추출 가능 여부
- 외부 요청 주체를 `ExternalUserRef` 같은 값으로 변환

`core`에서 하지 않을 일:

- 외부 ID를 내부 유저 ID로 변환
- Discord 연동 매핑 조회
- 레거시 인증 서버 호출
- `UserService` 같은 application service 호출
- 유저 엔티티 변경

예시:

```kotlin
sealed interface ExternalUserRef {
    data class Discord(
        val discordUserId: String,
    ) : ExternalUserRef

    data class LegacyAuth(
        val legacyUserId: String,
    ) : ExternalUserRef
}
```

### controllers

`controllers`는 외부 요청을 받아 해당 요청이 유스케이스에서 요구하는 형태인지 확인한다.

예상 책임:

- Discord 이벤트에서 `discordUserId` 추출
- HTTP 요청에서 인증 헤더, 세션, 요청자 식별자 추출
- 필요한 값이 없으면 transport-level 에러로 처리
- service에 유스케이스 파라미터 전달

`controllers`에서 하지 않을 일:

- Discord 매핑 repository 직접 조회
- 내부 유저 엔티티 조회 후 도메인 정책 판단
- 특정 도메인의 인증 정책을 직접 구현

### service

`service`는 실제 유스케이스를 수행하며, 필요한 경우 내부 유저 정보를 조회한다.

예상 책임:

- 유스케이스에 필요한 내부 유저 식별
- 매핑된 유저가 없을 때의 동작 결정
  - 무시
  - 예외
  - 연동 유도
- 내부 유저 상태에 따른 도메인 정책 처리
- 유저, Discord, 알림, 포인트 등 도메인 변경 수행

service는 필요한 인증/식별 조회를 `auth` 패키지에 정의된 reader/resolver를 통해 수행한다.

### auth

`auth` 패키지는 인증과 식별 해석만 담당한다.

예상 책임:

- `ExternalUserRef`를 인증 관점의 내부 사용자 view로 해석
- Discord ID와 내부 유저 ID 매핑 조회
- 레거시 인증 서버를 통한 요청자 식별
- 인증에 필요한 최소 정보 제공

`auth`에서 하지 않을 일:

- 유저 프로필 변경
- 회원 상태 변경
- Discord 닉네임/역할 반영
- 포인트, 알림, 회비 등 다른 도메인 정책 수행
- 범용 회원 DTO 제공

## Auth User View

`auth`는 `User` 엔티티 전체에 의존하기보다 인증 관점의 읽기 전용 view를 제공하는 방향이 적절하다.

이유:

- 인증에 필요한 정보는 유저 엔티티 전체가 아니다.
- `auth`가 `Member` 전체를 알면 user 도메인의 세부 정책과 필드 변경에 과하게 결합된다.
- 다른 도메인이 인증을 위해 `Member` 전체를 받아가면 인증 경계가 흐려진다.
- 읽기 전용 view를 사용하면 `auth`가 유저 도메인 변경 로직을 수행하지 않도록 제한할 수 있다.

예시:

```kotlin
data class AuthUserView(
    val userId: Long,
    val status: AuthUserStatus,
    val roles: Set<AuthRole>,
)
```

Reader 예시:

```kotlin
interface AuthUserReader {
    fun findByUserId(userId: Long): AuthUserView?

    fun findByDiscordUserId(discordUserId: String): AuthUserView?

    fun findByExternalUserRef(externalUserRef: ExternalUserRef): AuthUserView?
}
```

`AuthUserView`에 들어갈 수 있는 정보:

- 내부 유저 ID
- 인증 가능 여부
- 계정 상태
- role/authority
- 인증 판단에 필요한 최소 메타데이터

`AuthUserView`에 넣지 않을 정보:

- 회원 프로필
- 학적 상태
- 회비 상태
- 포인트
- 알림 설정
- 특정 도메인 유스케이스에서만 필요한 값

## Discord 이벤트 처리 방향

현재 문제는 Discord 이벤트 핸들러가 `UserService`를 직접 호출한다는 점이다.

기존 흐름:

```text
UserDiscordEventController
-> UserDiscordEventHandler(core)
-> UserService(service)
-> DiscordIntegrationRepository
-> DiscordMemberService
```

변경 방향:

```text
UserDiscordEventController
-> ExternalUserRef.Discord 생성
-> service에 discordUserId 또는 ExternalUserRef 전달
-> service에서 AuthUserReader를 통해 내부 유저 view 조회
-> service에서 필요한 도메인 처리 수행
```

이렇게 하면 `core`는 Discord 이벤트의 요청 주체만 표현하고, Discord 매핑 정보는 `auth` 또는 관련 reader 구현 내부에 남는다.

## 트레이드오프

장점:

- `core`가 `service`에 의존하지 않는다.
- Discord 매핑 정보가 `core`로 올라오지 않는다.
- HTTP 레거시 인증과 Discord 인증을 같은 요청 주체 모델로 다룰 수 있다.
- 인증에 필요한 유저 정보와 도메인 변경용 유저 엔티티를 분리할 수 있다.
- 인증 관련 중복 코드를 `auth` 패키지로 모을 수 있다.

단점:

- `ExternalUserRef`, `AuthUserView`, reader/resolver 같은 타입이 추가된다.
- 단순한 유스케이스에서는 직접 호출보다 흐름이 길어 보일 수 있다.
- `auth`가 과도하게 커지면 모든 도메인이 의존하는 중앙 허브가 될 수 있다.
- `AuthUserView`가 범용 유저 DTO처럼 커지지 않도록 지속적인 관리가 필요하다.

## 주의사항

- `auth`는 인증/식별 해석까지만 담당한다.
- `auth`는 user 도메인의 변경 정책을 대신 수행하지 않는다.
- `AuthUserView`는 인증에 필요한 최소 정보만 담는다.
- 특정 도메인에서 필요한 유저 상세 정보는 해당 service가 user 도메인의 적절한 reader/service를 통해 별도로 조회한다.
- Discord 연동 매핑 정보는 `core`가 알지 않는다.
- `core`는 외부 요청 주체의 존재와 형태만 표현한다.

