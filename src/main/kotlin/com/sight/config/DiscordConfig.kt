package com.sight.config

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.requests.GatewayIntent
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestTemplate
import java.time.Duration

@Configuration
class DiscordConfig {
    @Value("\${discord.api.base-url:https://discord.com/api/v10}")
    private val baseUrl: String = "https://discord.com/api/v10"

    @Value("\${discord.api.timeout:5000}")
    private val timeout: Int = 5000

    @Bean
    fun jda(
        @Value("\${discord.token}") token: String,
    ): JDA {
        return JDABuilder.createDefault(token)
            .enableIntents(GatewayIntent.GUILD_MEMBERS)
            .build()
    }

    @Bean
    fun discordRestTemplate(): RestTemplate {
        val factory =
            SimpleClientHttpRequestFactory().apply {
                setConnectTimeout(Duration.ofMillis(timeout.toLong()))
                setReadTimeout(Duration.ofMillis(timeout.toLong()))
            }

        return RestTemplate(factory)
    }

    fun getBaseUrl(): String = baseUrl
}
