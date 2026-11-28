package com.sight.core.config

private const val DEFAULT_FILE_MAX_SIZE_BYTES: Long = 20L * 1024 * 1024

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
    FILE_UPLOAD_MAX_SIZE_BYTES(
        defaultValue = DEFAULT_FILE_MAX_SIZE_BYTES.toString(),
        description = "공통 업로드 파일 하나의 최대 크기(바이트)",
    ),
    GROUP_RECORD_FILE_MAX_SIZE_BYTES(
        defaultValue = DEFAULT_FILE_MAX_SIZE_BYTES.toString(),
        description = "그룹 기록·임시저장 첨부 파일 하나의 최대 크기(바이트)",
    ),
    GROUP_ACTIVITY_REPORT_FILE_MAX_SIZE_BYTES(
        defaultValue = DEFAULT_FILE_MAX_SIZE_BYTES.toString(),
        description = "그룹 활동보고 파일 하나의 최대 크기(바이트)",
    ),
    GROUP_CARD_COVER_IMAGE_MAX_SIZE_BYTES(
        defaultValue = DEFAULT_FILE_MAX_SIZE_BYTES.toString(),
        description = "그룹 카드 커버 이미지 하나의 최대 크기(바이트)",
    ),
}
