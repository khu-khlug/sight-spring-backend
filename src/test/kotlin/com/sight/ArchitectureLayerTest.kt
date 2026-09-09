package com.sight

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.architecture.KoArchitectureCreator
import com.lemonappdev.konsist.api.architecture.Layer
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class ArchitectureLayerTest {
    @Test
    fun `프로젝트 계층 의존성 방향을 강제한다`() {
        with(KoArchitectureCreator) {
            Konsist.scopeFromDirectory("src/main/kotlin").assertArchitecture {
                val controllers = Layer("controllers", "com.sight.controllers..")
                val service = Layer("service", "com.sight.service..")
                val domain = Layer("domain", "com.sight.domain..")
                val config = Layer("config", "com.sight.config..")
                val core = Layer("core", "com.sight.core..")
                val repository = Layer("repository", "com.sight.repository..")

                service.doesNotDependOn(controllers)

                domain.doesNotDependOn(controllers)
                domain.doesNotDependOn(service)

                config.doesNotDependOn(controllers)
                config.doesNotDependOn(service)
                config.doesNotDependOn(domain)

                core.doesNotDependOn(controllers)
                core.doesNotDependOn(service)
                core.doesNotDependOn(domain)

                repository.doesNotDependOn(controllers)
                repository.doesNotDependOn(service)
            }
        }
    }

    @Test
    fun `컨트롤러 구현체는 domain과 repository를 직접 참조하지 않는다`() {
        val controllerFiles = controllerImplementationFiles()

        assertNoImportsFrom(controllerFiles, "com.sight.domain")
        assertNoImportsFrom(controllerFiles, "com.sight.repository")
    }

    @Test
    fun `서비스는 Spring MVC와 Servlet 타입을 참조하지 않는다`() {
        val serviceFiles =
            Konsist.scopeFromDirectory("src/main/kotlin")
                .files
                .filter { it.packagee?.name?.startsWith("com.sight.service") == true }

        assertNoImportsFrom(serviceFiles, "org.springframework.web.servlet")
        assertNoImportsFrom(serviceFiles, "jakarta.servlet")
    }

    private fun controllerImplementationFiles() =
        Konsist.scopeFromDirectory("src/main/kotlin")
            .files
            .filter {
                it.packagee?.name == "com.sight.controllers.http" ||
                    it.packagee?.name == "com.sight.controllers.discord"
            }

    private fun assertNoImportsFrom(
        files: List<com.lemonappdev.konsist.api.declaration.KoFileDeclaration>,
        prohibitedPackage: String,
    ) {
        val violations =
            files.filter { file ->
                file.imports.any { it.name.startsWith(prohibitedPackage) }
            }

        assertTrue(
            violations.isEmpty(),
            "${prohibitedPackage}를 직접 참조하는 파일: ${violations.joinToString { it.name }}",
        )
    }
}
