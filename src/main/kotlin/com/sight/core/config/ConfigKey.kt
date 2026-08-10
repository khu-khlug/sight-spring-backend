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
        description = "도서 스캔 허용 IP(동방 공유기의 외부 IP)",
    ),
    SMS_SENDER_PHONE(
        defaultValue = "",
        description = "문자 발송에 사용하는 동아리 공식 전화번호",
    ),
}
