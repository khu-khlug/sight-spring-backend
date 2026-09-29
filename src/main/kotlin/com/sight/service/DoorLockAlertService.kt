package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.core.room.CLUB_ROOM_LOCATIONS
import com.sight.service.discord.DiscordWebhookAdapter
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class DoorLockAlertService(
    private val discordWebhookAdapter: DiscordWebhookAdapter,
) {
    private val logger = LoggerFactory.getLogger(DoorLockAlertService::class.java)

    suspend fun alertDie(roomNumber: Int) {
        if (roomNumber !in CLUB_ROOM_LOCATIONS) {
            throw BadRequestException("유효하지 않은 방 번호입니다")
        }

        try {
            val payload = mapOf("content" to "${roomNumber}호 $ALERT_MESSAGE")
            discordWebhookAdapter.sendDoorLockAlert(payload)
        } catch (e: Exception) {
            logger.error("도어락 데몬 응답 없음 알림 전송 실패", e)
        }
    }

    companion object {
        private const val ALERT_MESSAGE = "🔴 도어락 데몬 응답 없음 - 라즈베리파이 또는 Flask 데몬 상태를 확인하세요."
    }
}
