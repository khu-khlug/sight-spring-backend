package com.sight.config

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.web.client.RestTemplate
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SolapiConfigTest {
    private val contextRunner =
        ApplicationContextRunner()
            .withUserConfiguration(SolapiConfig::class.java)
            .withPropertyValues(
                "solapi.api-key=test-api-key",
                "solapi.api-secret=test-api-secret",
                "solapi.base-url=https://api.solapi.test",
                "solapi.timeout=10s",
            )

    @Test
    fun `SOLAPI 환경 설정과 전용 RestTemplate Bean을 생성한다`() {
        contextRunner.run { context ->
            val properties = context.getBean(SolapiProperties::class.java)

            assertEquals("test-api-key", properties.apiKey)
            assertEquals("test-api-secret", properties.apiSecret)
            assertEquals("https://api.solapi.test", properties.baseUrl)
            assertEquals(Duration.ofSeconds(10), properties.timeout)
            assertNotNull(context.getBean("solapiRestTemplate", RestTemplate::class.java))
        }
    }
}
