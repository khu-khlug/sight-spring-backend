FROM golang:1.23.4-alpine3.20 AS builder

WORKDIR /workspace

COPY tasks/STANDARD.md tasks/STANDARD.md
COPY tools/task-lint tools/task-lint

WORKDIR /workspace/tools/task-lint

RUN go test ./...

RUN set -eu; \
    source_hash="$(go run ./cmd/task-lint --root /workspace --print-source-hash)"; \
    mkdir -p /out; \
    CGO_ENABLED=0 GOOS=darwin GOARCH=arm64 go build -trimpath \
      -ldflags="-s -w -X main.buildSourceHash=${source_hash}" \
      -o /out/task-lint-darwin-arm64 ./cmd/task-lint; \
    CGO_ENABLED=0 GOOS=darwin GOARCH=amd64 go build -trimpath \
      -ldflags="-s -w -X main.buildSourceHash=${source_hash}" \
      -o /out/task-lint-darwin-amd64 ./cmd/task-lint; \
    CGO_ENABLED=0 GOOS=windows GOARCH=amd64 go build -trimpath \
      -ldflags="-s -w -X main.buildSourceHash=${source_hash}" \
      -o /out/task-lint-windows-amd64.exe ./cmd/task-lint; \
    CGO_ENABLED=0 GOOS=windows GOARCH=arm64 go build -trimpath \
      -ldflags="-s -w -X main.buildSourceHash=${source_hash}" \
      -o /out/task-lint-windows-arm64.exe ./cmd/task-lint; \
    CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build -trimpath \
      -ldflags="-s -w -X main.buildSourceHash=${source_hash}" \
      -o /out/task-lint-linux-amd64 ./cmd/task-lint; \
    CGO_ENABLED=0 GOOS=linux GOARCH=arm64 go build -trimpath \
      -ldflags="-s -w -X main.buildSourceHash=${source_hash}" \
      -o /out/task-lint-linux-arm64 ./cmd/task-lint

FROM scratch AS binaries

COPY --from=builder /out/ /
