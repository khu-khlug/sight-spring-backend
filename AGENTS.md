## 작업 지침

`src/main/kotlin/com/sight` 아래의 코드를 새로 만들거나 수정하기 전에, 수정할 파일이 속한 레이어의 규칙 파일을 처음부터 끝까지 읽고 따른다.

- `config`의 파일을 수정할 때는 `rules/config.md`를 읽는다.
- `controllers`의 파일을 수정할 때는 `rules/controllers.md`를 읽는다.
- HTTP API의 method 또는 path를 결정하거나 수정할 때는 `rules/rest-api.md`를 읽는다.
- `core`의 파일을 수정할 때는 `rules/core.md`를 읽는다.
- `domain`의 파일을 수정할 때는 `rules/domain.md`를 읽는다.
- `repository`의 파일을 수정할 때는 `rules/repository.md`를 읽는다.
- `service`의 파일을 수정할 때는 `rules/service.md`를 읽는다.
- `src/test/kotlin/com/sight` 아래의 테스트를 새로 만들거나 수정할 때는 `rules/testing.md`를 읽는다.

두 개 이상 레이어의 파일을 수정할 때는 관련된 모든 규칙 파일을 읽는다. 두 규칙이 서로 다르면, 더 넓은 동작을 금지하는 규칙을 따른다.

## 오류 메시지

- 사용자에게 노출될 수 있는 모든 오류 메시지는 한국어로 작성한다.

## Task 문서 작업 지침

- `tasks/` 아래의 Task 문서를 새로 만들거나 수정하기 전에 `tasks/STANDARD.md`를 처음부터 끝까지 읽고 따른다.
- 새 Task 문서는 `tasks/STANDARD.md`의 필수 섹션을 따라 `tasks/open/` 아래에 만든다.
- Task 문서를 새로 만들거나 수정한 뒤에는 저장소 root에서 현재 OS와 architecture에 맞는 `tools/task-lint/bin/task-lint-*` binary를 실행한다.
- 현재 환경에 맞는 binary가 없거나 source hash가 일치하지 않으면 저장소 root에서 다음 명령을 실행하고 lint를 다시 실행한다.

```text
docker buildx build --file tools/task-lint/Dockerfile --output type=local,dest=tools/task-lint/bin .
```

- Task lint가 성공해야 Task 문서 작업을 완료한 것으로 간주한다.
- `tools/task-lint/bin/` 아래의 생성된 binary는 수정하거나 Git에 포함하지 않는다.

## 커밋 지침

- 커밋에는 현재 작업과 관계없는 파일을 포함하지 않는다.
- 커밋 메시지는 Conventional Commit 형식을 사용하며 한국어로 작성한다.
- 사용자가 만든 기존 변경은 사용자의 명시적인 요청 없이 커밋에 포함하지 않는다.
