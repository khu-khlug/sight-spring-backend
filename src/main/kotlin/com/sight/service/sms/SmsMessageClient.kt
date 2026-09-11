package com.sight.service.sms

import com.sight.domain.sms.SmsMessageType

data class SendSmsMessage(
    val recipientIndex: String,
    val from: String,
    val to: String,
    val text: String,
    val type: SmsMessageType,
)

data class SmsMessageClientResult(
    val recipientIndex: String,
    val accepted: Boolean,
)

interface SmsMessageClient {
    fun send(messages: List<SendSmsMessage>): List<SmsMessageClientResult>
}

class SmsMessageClientException(
    message: String,
) : RuntimeException(message)
