package com.sight.core.discord

interface DiscordOAuth2Adapter {
    suspend fun getAccessToken(code: String): String

    suspend fun getCurrentUserId(accessToken: String): String

    fun createOAuth2Url(state: String): String
}
