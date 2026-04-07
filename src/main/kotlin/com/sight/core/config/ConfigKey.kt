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
    BOOK_SCAN_ALLOWED_NET_IP(
        defaultValue = "",
        description = "도서 스캔 허용 IP 대역 (CIDR 형식, 예: 192.168.1.0/24)",
    ),
}
