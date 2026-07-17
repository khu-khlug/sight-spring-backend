# Controller 레이어 규칙

## 역할

- Controller는 HTTP 요청 또는 Discord 이벤트를 Service 메서드 호출로 바꾼다.
- Controller는 요청 형식 검증, 요청자 역할 확인, 응답 DTO 생성, HTTP 상태 코드 선언을 담당한다.
- Controller는 공개 API의 HTTP 메서드, 경로, 요청 형식, 응답 형식을 유지한다.

## 구현 규칙

- Controller 클래스는 `controllers` 패키지에 둔다.

## HTTP Controller 규칙

- HTTP Controller 클래스는 `controllers.http` 패키지에 둔다.
- HTTP 경로는 클래스가 아니라 각 메서드의 매핑 어노테이션에 전체 경로로 선언한다.
- HTTP 요청 본문과 HTTP 응답 본문은 Controller DTO로 표현한다.
- 요청 DTO에는 형식, 필수 여부, 값의 범위를 검증하는 규칙을 선언한다.
- Controller는 요청 DTO를 Service에 그대로 전달하지 않는다.
- Controller는 요청 DTO의 필드를 꺼내 Service 메서드의 인자로 전달한다.
- HTTP Controller는 `@Auth(...)` 어노테이션으로 인증을 요구하고, API가 허용하는 역할을 선언한다.
- HTTP Controller는 `@Auth(...)` 어노테이션으로 요청자가 일반 회원인지 운영진인지 확인한 뒤 Service를 호출한다.
- 인증된 사용자의 식별값은 명시적인 메서드 파라미터로 받는다.
- Service의 반환값은 Controller가 응답 DTO로 변환한다.
- HTTP 상태 코드는 Controller가 명시한다.

## Discord 이벤트 Controller 규칙

- Discord 이벤트 Controller 클래스는 `controllers.discord` 패키지에 둔다.

## 금지 규칙

- Controller는 비즈니스 규칙을 결정하지 않는다.
- Controller는 도메인 모델의 상태 변경이 허용되는지 판단하지 않는다.
- Controller는 영속성 Entity를 요청 본문이나 응답 본문으로 사용하지 않는다.
- Controller는 `@Transactional`을 선언하지 않는다.
- Controller는 HTTP 요청 객체, HTTP 응답 객체, 세션 객체를 Service에 전달하지 않는다.

## 의존성 규칙

- Controller 구현체는 Service와 인증·인가 코드에 직접 의존할 수 있다.
- HTTP Controller 구현체는 Spring Web의 타입과 어노테이션, Bean Validation 타입, HTTP 응답 타입, Controller DTO에 직접 의존할 수 있다.
- Discord 이벤트 Controller 구현체는 Discord 이벤트 프레임워크 타입에 직접 의존할 수 있다.
- Controller 구현체는 Repository와 Domain 타입에 직접 의존하지 않는다.
- Controller DTO는 영속성 Entity를 필드 타입, 부모 타입, 구현 인터페이스로 사용하지 않는다.
