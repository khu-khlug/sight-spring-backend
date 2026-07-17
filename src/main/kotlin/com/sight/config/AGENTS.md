# Config 레이어 규칙

## 역할

- Config는 Spring `@Configuration`을 사용해 애플리케이션 시작에 필요한 Bean을 만들고 연결한다.
- Config는 환경 변수와 설정 파일의 값을 Bean 생성에 전달한다.
- Config는 보안, HTTP 처리, 데이터베이스 연결, 외부 API 클라이언트의 공통 설정을 선언한다.

## 구현 규칙

- Spring `@Configuration` 클래스와 이를 보조하는 클래스는 `config` 패키지에 둔다.
- Config는 Bean 생성, Bean 연결, 설정값 검증만 수행한다.
- 환경마다 달라지는 값은 설정 파일, 환경 변수, Spring Profile로 구분한다.
- 애플리케이션 실행에 반드시 필요한 설정값은 시작 시점에 누락 여부를 확인한다.
- 외부 API 클라이언트의 주소, 인증 정보, 시간 제한 같은 연결 설정은 Config에서 전달한다.
- 공통 보안 정책과 공통 웹 정책은 Config에서 선언한다.

## 금지 규칙

- Config는 비즈니스 규칙을 작성하지 않는다.
- Config는 도메인 모델을 조회하거나 변경하지 않는다.
- Config는 다른 레이어에 의존하지 않는다.
