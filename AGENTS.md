## 작업 지침

`src/main/kotlin/com/sight` 아래의 코드를 새로 만들거나 수정하기 전에, 수정할 파일이 속한 레이어의 규칙 파일을 처음부터 끝까지 읽고 따른다.

- `config`의 파일을 수정할 때는 `rules/config.md`를 읽는다.
- `controllers`의 파일을 수정할 때는 `rules/controllers.md`를 읽는다.
- HTTP API의 method 또는 path를 결정하거나 수정할 때는 `rules/rest-api.md`를 읽는다.
- `core`의 파일을 수정할 때는 `rules/core.md`를 읽는다.
- `domain`의 파일을 수정할 때는 `rules/domain.md`를 읽는다.
- `repository`의 파일을 수정할 때는 `rules/repository.md`를 읽는다.
- `service`의 파일을 수정할 때는 `rules/service.md`를 읽는다.

두 개 이상 레이어의 파일을 수정할 때는 관련된 모든 규칙 파일을 읽는다. 두 규칙이 서로 다르면, 더 넓은 동작을 금지하는 규칙을 따른다.

## 커밋 지침

- 커밋에는 현재 작업과 관계없는 파일을 포함하지 않는다.
- 커밋 메시지는 Conventional Commit 형식을 사용하며 한국어로 작성한다.
- 사용자가 만든 기존 변경은 사용자의 명시적인 요청 없이 커밋에 포함하지 않는다.
