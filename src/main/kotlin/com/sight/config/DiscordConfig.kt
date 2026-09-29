package com.sight.config

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.requests.GatewayIntent
import org.apache.hc.client5.http.config.RequestConfig
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager
import org.apache.hc.core5.util.Timeout
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.RestTemplate

@Configuration
class DiscordConfig {
    @Value("\${discord.api.base-url:https://discord.com/api/v10}")
    private val baseUrl: String = "https://discord.com/api/v10"

    @Value("\${discord.api.timeout:5000}")
    private val timeout: Int = 5000

    @Bean
    @ConditionalOnProperty(name = ["discord.enabled"], havingValue = "true", matchIfMissing = true)
    fun jda(
        @Value("\${discord.token}") token: String,
    ): JDA {
        return JDABuilder.createDefault(token)
            .enableIntents(GatewayIntent.GUILD_MEMBERS)
            .build()
    }

    @Bean
    fun discordRestTemplate(): RestTemplate {
        val connectionManager =
            PoolingHttpClientConnectionManager().apply {
                maxTotal = 100
                defaultMaxPerRoute = 100
            }
        val requestConfig =
            RequestConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(timeout.toLong()))
                .setResponseTimeout(Timeout.ofMilliseconds(timeout.toLong()))
                .build()
        val httpClient =
            HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build()
        val factory = HttpComponentsClientHttpRequestFactory(httpClient)

        return RestTemplate(factory)
    }

    fun getBaseUrl(): String = baseUrl
}
