# Repository 레이어 규칙

## 역할

- Repository는 데이터베이스에 Domain 객체를 저장하고 다시 읽는다.
- Repository는 Service가 필요한 데이터를 조회할 수 있는 메서드를 제공한다.
- Repository는 SQL, JPA, QueryDSL 같은 데이터베이스 접근 방식을 Repository 안에 숨긴다.

## 구현 규칙

- Repository 인터페이스, 구현체, View는 `repository` 패키지에 둔다.
- Repository 메서드는 조회 조건, 정렬, 페이징에 필요한 값을 명시적인 인자로 받는다.
- 여러 테이블을 조인해 읽은 결과는 View로 표현할 수 있다.
- View는 조회 결과를 옮기는 데이터만 가진다.
- 목록 조회 메서드는 페이지네이션을 적용한다.
- 목록 조회 메서드는 offset 기반 페이지네이션 또는 cursor 기반 페이지네이션을 사용한다.
- offset 기반 페이지네이션은 `offset`과 `limit`을 파라미터 이름으로 사용한다.
- cursor 기반 페이지네이션은 `cursor`와 `limit`을 파라미터 이름으로 사용한다.
- 비즈니스 규칙으로 데이터 개수의 상한이 정해져 있거나, 데이터가 늘어나지 않거나, 한 번에 조회할 데이터가 100개 미만일 가능성이 매우 높을 때만 전체 조회를 허용한다.
- 복잡한 조회 메서드는 무엇을 조회하는지와 정렬·페이징 조건을 이름으로 드러낸다.

## 데이터 관리 규칙

- 영속화되는 Domain 모델의 삭제 방식은 Repository 구현이 결정한다.
- Repository 구현은 물리적 삭제와 soft delete 중 Domain 모델에 맞는 방식을 사용한다.
- Repository 인터페이스와 삭제 메서드 이름은 물리적 삭제인지 soft delete인지 노출하지 않는다.
- 삭제된 Domain 모델은 일반 조회 결과에 포함하지 않는다.
- Service는 삭제 요청의 성공 여부와 삭제된 Domain 모델이 일반 조회에서 제외된다는 사실만 안다.

## 금지 규칙

- Repository는 비즈니스 규칙을 결정하지 않는다.
- Repository는 유스케이스 전체의 트랜잭션 시작과 종료를 결정하지 않는다.

## 의존성 규칙

- Repository는 Domain 모델, JPA와 QueryDSL 같은 데이터베이스 라이브러리, View에 의존할 수 있다.
- Repository 구현 방식은 `repository` 패키지 밖으로 노출하지 않는다.
