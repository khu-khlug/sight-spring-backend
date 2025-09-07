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
- `com.sight` 하위에는 다음 6개 디렉토리만 유지:
  - `config`: 애플리케이션 설정
  - `controllers`: 외부 요청 처리
  - `core`: 횡단 관심사
  - `domain`: 도메인 모델 및 서비스
  - `repository`: 데이터 접근 계층
  - `service`: 애플리케이션 서비스 계층

### 디렉토리별 책임
- **config**: 애플리케이션 설정과 관련된 내용. 로직이 들어가면 안 됨
- **controllers**: 외부에서 들어오는 모든 요청(HTTP, 디스코드 이벤트 등)을 최초로 받는 곳
  - REST API 컨트롤러: `com.sight.controllers.http` 패키지
  - 디스코드 이벤트 컨트롤러: `com.sight.controllers.discord` 패키지
  - **중요**: 컨트롤러에서는 비즈니스 로직을 작성하지 않고, DTO validation과 응답 DTO 생성만 담당
  - 모든 비즈니스 로직은 Service 계층에 위임
  - **API 경로 규칙**: 클래스 레벨 `@RequestMapping` 사용 금지. 각 메서드의 매핑 어노테이션에 전체 경로 직접 지정 (예: `@GetMapping("/users/@me/profile")`)
- **core**: 횡단 관심사와 관련된 모든 코드 (데이터베이스 연결, 인증/인가, 디스코드 클라이언트 등)
- **domain**: 도메인 모델과 도메인 서비스 (순수 함수, side-effect 없음)
  - 멤버 도메인: `com.sight.domain.member` 패키지
- **repository**: 데이터 접근을 위한 리포지토리 인터페이스
- **service**: side-effect가 발생할 수 있는 로직 및 애플리케이션 처리 흐름 제어 (clean architecture의 application 계층)
  - 컨트롤러에서 호출되는 모든 비즈니스 로직을 담당
  - 아무리 간단한 로직이라도 Service 계층에서 처리

### 의존성 방향
- 일반적인 로직 처리 흐름: `controllers` → `service` → `domain`
- 역방향 의존성은 금지
- `config`, `core`, `repository`는 다른 계층에서 접근 가능하지만 역방향은 불가
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

### 에러 메시지 작성 규칙
- **모든 에러 메시지는 한국어로 작성**: ResponseStatusException, IllegalStateException 등의 메시지는 한국어 사용
- 사용자에게 보여질 수 있는 모든 오류 문구는 한국어로 작성
- 예시:
  - "Authentication required" → "인증이 필요합니다"
  - "Insufficient privileges" → "권한이 부족합니다"
  - "User not found" → "사용자를 찾을 수 없습니다"
  - "No authenticated user found" → "인증된 사용자가 없습니다"

### 개발 참조
- 다른 로컬 프로젝트를 참조할 때는 `-ref` 접미사로 심볼릭 링크 생성 (예: `ln -s /path/to/other-project ./other-project-ref`)
- 모든 `*-ref` 디렉토리는 git에서 자동으로 무시됨
- 개발 중 관련 프로젝트를 연결할 때 이 패턴 사용