package com.sight.core.config

enum class ConfigKey(
    val defaultValue: String,
    val description: String,
) {
    KHLUG_ACCOUNT_NUMBER(
        defaultValue = "",
        description = "동아리 계좌 번호",
    ),
}
