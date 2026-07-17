# Service 레이어 규칙

## 역할

- Service는 API나 이벤트가 요청한 하나의 업무 기능을 처음부터 끝까지 처리한다.
- Service는 데이터를 읽고 저장하는 순서, Domain 규칙 호출 순서, 외부 시스템 호출 순서를 결정한다.
- Service는 하나의 업무 기능에서 데이터베이스 변경과 외부 시스템 호출이 함께 일어날 때 실패 처리 순서를 결정한다.

## 구현 규칙

- Service 구현체와 Service 전용 DTO는 `service` 패키지에 둔다.
- Service의 공개 메서드 이름은 수행하는 업무 기능을 드러낸다.
- 데이터를 저장, 수정, 삭제하는 Service 메서드는 트랜잭션을 선언한다.
- 데이터를 읽기만 하는 Service 메서드는 읽기 전용 트랜잭션을 선언하고 데이터를 변경하지 않는다.
- Service는 외부 입력값을 Domain 값 또는 Service 전용 입력 DTO로 바꾼다.
- Service는 업무 규칙 위반과 요청을 처리하기 위한 사전 조건 위반을 구분해 처리한다.
- Service는 외부 시스템을 호출하기 전에 저장할 데이터와 권한을 확인한다.
- 데이터베이스 변경 뒤 외부 시스템 호출이 실패할 수 있으면, 재시도, 보상 처리, 오류 기록 중 필요한 방법을 정한다.
- Service는 Repository와 외부 시스템 호출 인터페이스를 생성자 주입으로 받는다.

## 금지 규칙

- Service는 HTTP 요청·응답 객체, 세션, 쿠키, Servlet 타입을 사용하지 않는다.
- Service는 HTTP 상태 코드와 HTTP 응답 형식을 결정하지 않는다.
- Service는 Controller DTO를 입력값이나 반환값으로 사용하지 않는다.
- Service는 Controller를 호출하지 않는다.
- Service는 Repository 구현체를 직접 만들지 않는다.
- Service는 외부 API의 HTTP 요청과 응답 형식을 직접 구현하지 않는다.
- Service는 화면 표시 형식과 JSON 직렬화 형식을 결정하지 않는다.
- Service는 Domain 객체가 보장해야 하는 상태 변경 규칙을 중복해서 작성하지 않는다.

## 의존성 규칙

- Service는 Domain, Repository, 외부 시스템 호출 인터페이스, 인증·예외 처리 같은 공통 코드에 의존할 수 있다.
- Service는 Controller와 HTTP·Servlet 라이브러리에 의존하지 않는다.
- Service는 외부 시스템 통신 구현체가 아니라 외부 시스템 호출 인터페이스에 의존한다.
- Service 전용 DTO는 해당 업무 기능의 입력값이나 반환값만 가진다.
