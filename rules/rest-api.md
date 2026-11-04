# REST API Method 및 Path 규칙

## 기본 원칙

- API path는 리소스를 표현하는 명사를 사용한다.
- 리소스 이름은 복수형을 사용한다.
- path에는 동사를 사용하지 않는다.

## HTTP Method

- `GET`: 리소스를 조회한다.
- `POST`: 리소스를 생성한다.
- `PUT`: 리소스를 전체 교체한다.
- `PATCH`: 리소스를 일부 수정한다.
- `DELETE`: 리소스를 삭제한다.

## Path 구성

- 운영진 전용 API는 `/manager` prefix를 사용한다.
  - 예: `/manager/users/{userId}`
- 단일 리소스 조회, 수정, 삭제에는 path variable을 사용한다.
  - 예: `/users/{userId}`
- path variable에 사용하는 식별자는 해당 리소스의 실제 PK만 허용한다.
  - 유일 식별자이더라도 PK가 아닌 값은 path variable로 사용하지 않는다.
- 리소스의 실제 하위 리소스에는 중첩 path를 사용할 수 있다.
  - 예: `/users/{userId}/posts`
- 중첩 path와 query parameter 중 어떤 방식을 사용할지는 조회 목적과 이후 확장 방향을 고려해 명확히 결정하고, 개발자는 그 요구사항을 제시해야 한다.
  - 모든 그룹의 멤버를 조회하되 그룹을 필터링하는 경우: `GET /group-members?groupId=...`
  - 특정 그룹의 모든 멤버를 조회하는 경우: `GET /groups/{groupId}/members`
  - 두 방식이 현재 같은 결과를 반환하더라도, 이후 필터와 조회 범위의 확장 방향에 영향을 주므로 목적을 구분한다.
- 필터, 정렬, 페이지네이션은 query parameter를 사용한다.
  - 예: `/posts?status=published&offset=0&limit=20`

## 페이지네이션

- 목록 조회 API에는 기본적으로 페이지네이션을 적용한다.
- offset-based pagination과 cursor-based pagination을 허용한다.
- offset-based pagination은 `offset`, `limit` query parameter를 사용한다.
- cursor-based pagination은 `cursor`, `limit` query parameter를 사용한다.
- `limit`은 100을 초과할 수 없다.
- 다음 조건 중 하나를 만족하는 경우에만 페이지네이션을 적용하지 않을 수 있다.
  - 개수가 늘어날 가능성이 없다.
  - 개수를 늘리려면 사전 논의를 거쳐야 한다.
  - 개수가 1년에 3~5개 정도만 늘어난다.
  - 그 밖에 반환 개수가 절대 100개를 초과하지 않음을 증명할 수 있다.

## 명명 규칙

- path는 소문자와 kebab-case를 사용한다.
  - 권장: `/user-profiles`
  - 비권장: `/userProfiles`
- 식별자 이름은 `{resource}Id` 형식을 사용한다.
  - 예: `/users/{userId}`

## 예외적인 행위

- CRUD로 자연스럽게 표현하기 어려운 행위는 action path를 제한적으로 허용한다.
  - 예: `POST /posts/{postId}/publish`
- 단순 조회, 생성, 수정, 삭제에는 action path를 사용하지 않는다.

## 주의사항

- URL에 민감한 정보를 포함하지 않는다.
- 중첩 path는 필요한 경우에만 사용하며, 과도하게 깊게 만들지 않는다.
- 같은 의미의 리소스에는 일관된 path와 method 규칙을 적용한다.

## 규칙 예외

- 위 규칙을 지키기 어렵다고 판단하면 그 이유를 PR에 명확히 작성해, 리뷰 과정에서 논의할 수 있도록 한다.
