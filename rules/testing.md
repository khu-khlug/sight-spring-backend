# 테스트 규칙

## 공통 규칙

- 테스트 메서드명은 한글 백틱을 사용해 테스트 대상 동작을 설명한다.
- 예외 테스트에는 Kotlin의 `assertThrows<예외타입>`을 사용한다.

## Controller 테스트 규칙

- HTTP Controller 테스트는 `@WebMvcTest`와 `MockMvc`를 사용한다.
- Controller 클래스는 전체 패키지 경로를 사용해 지정한다.

## Service 테스트 규칙

- Mock 객체는 Mockito 어노테이션 대신 `mock<타입>()` 함수로 만든다.
- Service 인스턴스는 생성자에 Mock 객체를 직접 전달해 만든다.
- Stubbing에는 `when().thenReturn()` 대신 `given().willReturn()` 패턴을 사용한다.
- Mock 호출 검증에는 `verify()`를 사용한다.
