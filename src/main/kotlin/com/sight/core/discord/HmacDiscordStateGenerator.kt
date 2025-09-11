package com.sight.core.discord

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Component
class HmacDiscordStateGenerator(
    @Value("\${discord.oauth2.state-secret}")
    private val stateSecret: String,
) : DiscordStateGenerator {
    override fun generate(userId: Long): String {
        val mac = Mac.getInstance("HmacSHA256")
        val secretKeySpec = SecretKeySpec(stateSecret.toByteArray(), "HmacSHA256")
        mac.init(secretKeySpec)

        val data = userId.toString().toByteArray()
        val hmacBytes = mac.doFinal(data)

        return hmacBytes.joinToString("") { "%02x".format(it) }
    }

    override fun validate(
        userId: Long,
        state: String,
    ): Boolean {
        val expectedState = generate(userId)
        return MessageDigest.isEqual(expectedState.toByteArray(), state.toByteArray())
    }
}
