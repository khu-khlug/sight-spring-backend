package com.sight.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestTemplate
import java.time.Duration

@ConfigurationProperties(prefix = "solapi")
data class SolapiProperties(
    val apiKey: String = "",
    val apiSecret: String = "",
    val baseUrl: String = "https://api.solapi.com",
    val timeout: Duration = Duration.ofSeconds(10),
)

@Configuration
@EnableConfigurationProperties(SolapiProperties::class)
class SolapiConfig(
    private val properties: SolapiProperties,
) {
    @Bean
    fun solapiRestTemplate(): RestTemplate {
        val requestFactory =
            SimpleClientHttpRequestFactory().apply {
                setConnectTimeout(properties.timeout)
                setReadTimeout(properties.timeout)
            }
        return RestTemplate(requestFactory)
    }
}
