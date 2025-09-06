# CLAUDE.md

이 파일은 Claude Code(claude.ai/code)가 이 저장소의 코드 작업을 수행할 때 참고하는 가이드를 제공합니다.

## 개발 명령어

### 빌드 및 실행
- `./gradlew build` - 프로젝트 빌드 및 테스트 실행
- `./gradlew bootRun` - 개발 서버 시작 (http://localhost:8080에서 실행)
- `docker compose up` - Docker로 애플리케이션 실행 (라이브 리로드 포함)

### 테스팅
- `./gradlew test` - 모든 테스트 실행
- `./gradlew test --tests "com.sight.controllers.api.PingControllerTest"` - 특정 테스트 클래스 실행
- `./gradlew test --tests "*ping*"` - 패턴에 맞는 테스트 실행

### 코드 품질
- `./gradlew ktlintCheck` - Kotlin 코드 스타일 검사
- `./gradlew ktlintFormat` - Kotlin 코드 자동 포맷팅
- `./gradlew check` - 테스트 및 ktlint를 포함한 모든 검사 실행

### Docker
- `docker build -f Dockerfile.Build -t sight-spring-backend .` - 프로덕션 이미지 빌드
- `docker build -f Dockerfile.Local .` - 로컬 개발 이미지 빌드

## 아키텍처

### 기술 스택
- **언어**: Kotlin 1.9.25
- **프레임워크**: Spring Boot 3.3.5
- **런타임**: Java 17
- **빌드 도구**: Gradle with Kotlin DSL
- **테스팅**: JUnit 5 with Spring Boot Test, Testcontainers
- **코드 스타일**: ktlint

### 프로젝트 구조
- 메인 애플리케이션 진입점: `src/main/kotlin/com/sight/SightSpringBackendApplication.kt`
- **컨트롤러 아키텍처**: 모든 외부 요청과 이벤트는 `controllers` 패키지에서 처리
  - REST API 컨트롤러: `com.sight.controllers.api` 패키지
  - 디스코드 이벤트 컨트롤러: `com.sight.controllers.discord` 패키지
  - API 엔드포인트는 `/api` 접두사 사용 (예: `/api/ping`)
- **코어 아키텍처**: 핵심 비즈니스 로직과 이벤트 처리는 `core` 패키지에서 담당
  - 디스코드 이벤트 핸들러: `com.sight.core.discord` 패키지
  - 컨트롤러는 실제 처리를 위해 코어 핸들러에 위임
- **도메인 아키텍처**: 도메인 모델은 기능별로 구성
  - 멤버 도메인: `com.sight.domain.member` 패키지
- 설정 프로파일: `local` (기본값), `prod`

### Spring Boot 설정
- 서버는 8080 포트에서 실행
- 액추에이터 엔드포인트 노출: health, info, metrics
- `application.yml`에서 프로파일별 로깅 레벨 설정
- 로컬 프로파일은 `com.sight` 패키지에 대해 DEBUG 로깅 활성화

### 테스팅 패턴
- 컨트롤러 테스트는 MockMvc와 함께 `@WebMvcTest` 사용
- 테스트 메서드명은 한글 백틱 사용: `` `ping API는 pong을 반환한다`() ``
- Spring Boot 테스팅 컨벤션을 따르며 `@Autowired` MockMvc 사용
- 컨트롤러 테스트 시 전체 패키지 경로 참조 (예: `com.sight.controllers.api.PingController::class`)

### 개발 환경
- Docker Compose는 볼륨 마운팅을 통한 라이브 리로드 제공
- Docker 환경에서 Gradle 데몬 비활성화
- Docker에서 로컬 프로파일 자동 활성화

### 개발 참조
- 다른 로컬 프로젝트를 참조할 때는 `-ref` 접미사로 심볼릭 링크 생성 (예: `ln -s /path/to/other-project ./other-project-ref`)
- 모든 `*-ref` 디렉토리는 git에서 자동으로 무시됨
- 개발 중 관련 프로젝트를 연결할 때 이 패턴 사용