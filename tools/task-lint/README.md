# Task lint

## Binary build

저장소 root에서 다음 명령을 실행한다.

```text
docker buildx build --file tools/task-lint/Dockerfile --output type=local,dest=tools/task-lint/bin .
```

생성된 binary는 `tools/task-lint/bin/`에 저장되며 Git에 포함하지 않는다.

## 실행

macOS Apple Silicon:

```text
tools/task-lint/bin/task-lint-darwin-arm64
```

macOS Intel:

```text
tools/task-lint/bin/task-lint-darwin-amd64
```

Windows x64:

```text
tools\task-lint\bin\task-lint-windows-amd64.exe
```

Linux x64:

```text
tools/task-lint/bin/task-lint-linux-amd64
```

lint source가 binary build 이후 변경되면 실행을 중단하고 binary를 다시 build하도록 안내한다.

## 검증 범위

`tasks/TEMPLATE.md`의 `##` heading을 필수 섹션과 순서의 기준으로 사용한다.

`tasks/open/`과 `tasks/completed/` 아래의 모든 Markdown 문서는 다음 조건을 만족해야 한다.

- 모든 필수 섹션이 정확히 한 번 존재한다.
- 필수 섹션끼리의 상대적 순서가 Template과 같다.
- 각 필수 섹션의 내용은 공백을 제외하고 한 글자 이상이다.

추가 `##` 섹션은 허용한다. fenced code block 안의 `##`는 섹션으로 인식하지 않는다.
