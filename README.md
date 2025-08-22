# Sight Spring Backend

쿠러그 사이트의 코틀린 / 스프링 기반 백엔드 레포지토리입니다.

## 요구사항

- Java 17
- Docker

## 설치 방법

```bash
git clone https://github.com/khu-khlug/sight-spring-backend.git
cd sight-spring-backend
./gradlew build
```

## 실행 방법

```bash
# 로컬 실행 시
./gradlew bootRun

# 도커 활용 시
docker compose up
```

애플리케이션은 `http://localhost:8080`에서 실행됩니다.

## 빌드 방법

```bash
# JAR 빌드 (`build/libs/` 하위에 생성됩니다.)
./gradlew build

# 도커 이미지 빌드
docker build -f Dockerfile.Build -t sight-spring-backend .
```

## 테스트 실행 방법

```bash
./gradlew test
```

## Lint 실행 방법

```bash
# Lint 검사
./gradlew ktlintCheck

# Lint 자동 수정
./gradlew ktlintFormat
```
