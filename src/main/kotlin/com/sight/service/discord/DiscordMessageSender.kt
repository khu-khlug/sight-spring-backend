package com.sight.service.discord

import net.dv8tion.jda.api.JDA
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

interface DiscordMessageSender {
    fun sendDirectMessage(
        discordUserId: String,
        content: String,
    )
}

@Component
class JdaDiscordMessageSender(
    @Autowired(required = false) private val jda: JDA?,
    @param:Value("\${discord.api.timeout:5000}") private val timeoutMillis: Long,
) : DiscordMessageSender {
    override fun sendDirectMessage(
        discordUserId: String,
        content: String,
    ) {
        val user = requireJda().retrieveUserById(discordUserId).submit().get(timeoutMillis, TimeUnit.MILLISECONDS)
        user.openPrivateChannel()
            .submit()
            .get(timeoutMillis, TimeUnit.MILLISECONDS)
            .sendMessage(content)
            .submit()
            .get(timeoutMillis, TimeUnit.MILLISECONDS)
    }

    private fun requireJda(): JDA = jda ?: throw IllegalStateException("DISCORD_ENABLED=false로 설정되어 Discord 기능을 사용할 수 없습니다.")
}
