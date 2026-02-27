package com.sight.core.config

enum class ConfigKey(
    val defaultValue: String,
    val description: String,
) {
    KHLUG_ACCOUNT_NUMBER(
        defaultValue = "",
        description = "동아리 계좌 번호",
    ),
    DOOR_LOCK_MASTER_PASSWORD(
        defaultValue = "",
        description = "도어락 마스터 비밀번호",
    ),
    DOOR_LOCK_JAJUDY_PASSWORD(
        defaultValue = "",
        description = "도어락 중동연용 비밀번호",
    ),
    DOOR_LOCK_FACILITY_TEAM_PASSWORD(
        defaultValue = "",
        description = "도어락 시설팀용 비밀번호",
    ),
}
