# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Development Commands

### Build and Run
- `./gradlew build` - Build the project and run tests
- `./gradlew bootRun` - Start the development server (runs on http://localhost:8080)
- `docker compose up` - Run the application in Docker with live reload

### Testing
- `./gradlew test` - Run all tests
- `./gradlew test --tests "com.sight.controller.PingControllerTest"` - Run specific test class
- `./gradlew test --tests "*ping*"` - Run tests matching pattern

### Code Quality
- `./gradlew ktlintCheck` - Check Kotlin code style
- `./gradlew ktlintFormat` - Auto-format Kotlin code
- `./gradlew check` - Run all checks including tests and ktlint

### Docker
- `docker build -f Dockerfile.Build -t sight-spring-backend .` - Build production image
- `docker build -f Dockerfile.Local .` - Build local development image

## Architecture

### Technology Stack
- **Language**: Kotlin 1.9.25
- **Framework**: Spring Boot 3.3.5
- **Runtime**: Java 17
- **Build Tool**: Gradle with Kotlin DSL
- **Testing**: JUnit 5 with Spring Boot Test, Testcontainers
- **Code Style**: ktlint

### Project Structure
- Main application entry point: `src/main/kotlin/com/sight/SightSpringBackendApplication.kt`
- Controllers follow REST conventions under `com.sight.controller` package
- API endpoints use `/api` prefix (e.g., `/api/ping`)
- Configuration profiles: `local` (default), `prod`

### Spring Boot Configuration
- Server runs on port 8080
- Actuator endpoints exposed: health, info, metrics
- Profile-specific logging levels configured in `application.yml`
- Local profile enables DEBUG logging for `com.sight` package

### Testing Patterns
- Use `@WebMvcTest` for controller tests with MockMvc
- Test method names use Korean backticks: `` `ping API는 pong을 반환한다`() ``
- Follow Spring Boot testing conventions with `@Autowired` MockMvc

### Development Environment
- Docker Compose provides live reload with volume mounting
- Gradle daemon disabled in Docker environment
- Local profile automatically activated in Docker

### Development References
- Create symbolic links with `-ref` suffix to reference other local projects (e.g., `ln -s /path/to/other-project ./other-project-ref`)
- All `*-ref` directories are automatically ignored by git
- Use this pattern for linking related projects during development