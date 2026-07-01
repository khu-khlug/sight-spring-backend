package com.sight

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.architecture.KoArchitectureCreator
import com.lemonappdev.konsist.api.architecture.Layer
import org.junit.jupiter.api.Test

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

                controllers.dependsOn(service)
                controllers.dependsOn(config)
                controllers.dependsOn(core)
                controllers.dependsOn(repository)

                service.dependsOn(domain)
                service.dependsOn(config)
                service.dependsOn(core)
                service.dependsOn(repository)

                domain.dependsOn(config)
                domain.dependsOn(core)
                domain.dependsOn(repository)

                config.doesNotDependOn(controllers)
                config.doesNotDependOn(service)
                config.doesNotDependOn(domain)

                core.doesNotDependOn(controllers)
                core.doesNotDependOn(service)
                core.doesNotDependOn(domain)

                repository.doesNotDependOn(controllers)
                repository.doesNotDependOn(service)
                repository.doesNotDependOn(domain)
            }
        }
    }
}
