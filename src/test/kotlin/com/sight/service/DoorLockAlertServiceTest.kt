package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.service.discord.DiscordWebhookAdapter
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class DoorLockAlertServiceTest {
    private val discordWebhookAdapter: DiscordWebhookAdapter = mock()
    private val service = DoorLockAlertService(discordWebhookAdapter)

    @Test
    fun `alertDie는 디스코드 웹훅으로 알림을 전송한다`() {
        runBlocking { service.alertDie(roomNumber = 405) }

        runBlocking { verify(discordWebhookAdapter).sendDoorLockAlert(any()) }
    }

    @Test
    fun `alertDie는 웹훅 전송 실패시 예외를 던지지 않는다`() {
        runBlocking {
            doThrow(RuntimeException("Webhook failed"))
                .whenever(discordWebhookAdapter)
                .sendDoorLockAlert(any())
        }

        runBlocking { service.alertDie(roomNumber = 405) }
    }

    @Test
    fun `alertDie는 유효하지 않은 방 번호면 예외를 던진다`() {
        assertThrows(BadRequestException::class.java) {
            runBlocking { service.alertDie(roomNumber = 999) }
        }

        runBlocking { verify(discordWebhookAdapter, never()).sendDoorLockAlert(any()) }
    }
}
