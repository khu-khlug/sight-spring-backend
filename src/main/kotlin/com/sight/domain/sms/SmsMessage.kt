package com.sight.domain.sms

enum class SmsMessageType {
    SMS,
    LMS,
}

data class SmsMessage private constructor(
    val text: String,
    val type: SmsMessageType,
) {
    companion object {
        private const val SMS_MAX_BYTES = 90
        private const val REALNAME_PLACEHOLDER = "{realname}"

        fun personalize(
            template: String,
            recipientName: String,
        ): SmsMessage {
            val text = template.replace(REALNAME_PLACEHOLDER, recipientName)
            val type = if (byteCount(text) <= SMS_MAX_BYTES) SmsMessageType.SMS else SmsMessageType.LMS
            return SmsMessage(text = text, type = type)
        }

        fun byteCount(text: String): Int =
            text.fold(0) { total, character ->
                total + if (character.code <= 0x7F) 1 else 2
            }
    }
}

object PhoneNumberNormalizer {
    fun normalize(phone: String?): String =
        phone
            .orEmpty()
            .filter { it in '0'..'9' }
}
