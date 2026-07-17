# Controller 레이어 규칙

## 역할

- Controller는 HTTP 요청 또는 Discord 이벤트를 Service 메서드 호출로 바꾼다.
- Controller는 요청 형식 검증, 요청자 확인, 권한 선언, 응답 DTO 생성, HTTP 상태 코드 선언을 담당한다.
- Controller는 공개 API의 HTTP 메서드, 경로, 요청 형식, 응답 형식을 유지한다.

## 구현 규칙

- Controller 클래스는 `controllers` 패키지에 둔다.
- HTTP Controller 클래스는 `controllers.http` 패키지에 둔다.
- Discord 이벤트 Controller 클래스는 `controllers.discord` 패키지에 둔다.
- HTTP 경로는 클래스가 아니라 각 메서드의 매핑 어노테이션에 전체 경로로 선언한다.
- HTTP 요청 본문과 HTTP 응답 본문은 Controller DTO로 표현한다.
- 요청 DTO에는 형식, 필수 여부, 값의 범위를 검증하는 규칙을 선언한다.
- 인증된 사용자의 식별값은 명시적인 메서드 파라미터로 받는다.
- API가 요구하는 권한은 메서드에 선언한다.
- Service의 반환값은 Controller가 응답 DTO로 변환한다.
- HTTP 상태 코드는 Controller가 명시한다.

## 금지 규칙

- Controller는 업무 규칙을 결정하지 않는다.
- Controller는 업무 데이터의 상태 변경이 허용되는지 판단하지 않는다.
- Controller는 Repository를 직접 호출하지 않는다.
- Controller는 영속성 Entity를 요청 본문이나 응답 본문으로 사용하지 않는다.
- Controller는 영속성 Entity를 저장, 수정, 삭제하지 않는다.
- Controller는 `@Transactional`을 선언하지 않는다.
- Controller는 외부 API나 메시지 시스템을 직접 호출하지 않는다.
- Controller는 다른 Controller를 호출하지 않는다.
- Controller는 HTTP 요청 객체, HTTP 응답 객체, 세션 객체를 Service에 전달하지 않는다.

## 의존성 규칙

- Controller 구현체는 Service와 인증·인가 코드에만 직접 의존한다.
- Controller 구현체는 Repository와 Domain 타입에 직접 의존하지 않는다.
- Controller DTO는 영속성 Entity를 필드 타입, 부모 타입, 구현 인터페이스로 사용하지 않는다.
