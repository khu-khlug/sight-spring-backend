# Repository 레이어 규칙

## 역할

- Repository는 데이터베이스에 Domain 객체를 저장하고 다시 읽는다.
- Repository는 Service가 필요한 데이터를 조회할 수 있는 메서드를 제공한다.
- Repository는 SQL, JPA, QueryDSL 같은 데이터베이스 접근 방식을 Repository 안에 숨긴다.

## 구현 규칙

- Repository 인터페이스, 구현체, 조회 전용 DTO, Projection은 `repository` 패키지에 둔다.
- Repository 메서드는 저장, 존재 확인, 단건 조회, 목록 조회, 개수·합계 같은 집계 중 하나의 목적을 가진다.
- Repository 메서드는 조회 조건, 정렬, 페이징에 필요한 값을 명시적인 인자로 받는다.
- 여러 테이블을 조인해 읽은 결과는 Repository 전용 DTO나 Projection으로 표현할 수 있다.
- Repository 전용 DTO와 Projection은 조회 결과를 옮기는 데이터만 가진다.
- 복잡한 조회 메서드는 무엇을 조회하는지와 정렬·페이징 조건을 이름으로 드러낸다.

## 금지 규칙

- Repository는 Controller와 Service를 참조하지 않는다.
- Repository는 Controller DTO를 반환값이나 인자로 사용하지 않는다.
- Repository는 HTTP 상태 코드와 HTTP 응답 형식을 결정하지 않는다.
- Repository는 요청자의 인증 정보나 권한을 판단하지 않는다.
- Repository는 외부 API와 메시지 시스템을 호출하지 않는다.
- Repository는 업무 데이터의 상태 변경 규칙을 결정하지 않는다.
- Repository는 유스케이스 전체의 트랜잭션 시작과 종료를 결정하지 않는다.

## 의존성 규칙

- Repository는 Domain 모델, JPA와 QueryDSL 같은 데이터베이스 라이브러리, Repository 전용 DTO와 Projection에 의존할 수 있다.
- Repository 구현 방식은 `repository` 패키지 밖으로 노출하지 않는다.
