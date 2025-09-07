package com.sight.discord.config

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.requests.GatewayIntent
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DiscordConfig {
    @Bean
    fun jda(
        @Value("\${discord.token}") token: String,
    ): JDA {
        return JDABuilder.createDefault(token)
            .enableIntents(GatewayIntent.GUILD_MEMBERS)
            .build()
    }
}
