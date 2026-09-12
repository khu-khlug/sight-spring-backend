package com.sight.service

import com.sight.core.config.ConfigKey
import com.sight.core.config.SystemConfigRegistry
import com.sight.core.exception.BadRequestException
import com.sight.core.exception.InternalServerErrorException
import com.sight.core.exception.NotFoundException
import com.sight.domain.sms.PhoneNumberNormalizer
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SenderPhoneService(
    private val systemConfigRegistry: SystemConfigRegistry,
) {
    @Transactional(readOnly = true)
    fun getSenderPhone(): String {
        return getFreshSenderPhone().ifBlank {
            throw NotFoundException("동아리 공식 발신번호가 설정되지 않았습니다")
        }
    }

    @Transactional
    fun updateSenderPhone(phone: String) {
        val normalizedPhone = PhoneNumberNormalizer.normalize(phone)
        if (normalizedPhone.isBlank()) {
            throw BadRequestException("동아리 공식 발신번호에는 숫자가 포함되어야 합니다")
        }
        systemConfigRegistry.setValue(ConfigKey.SMS_SENDER_PHONE, normalizedPhone)
    }

    @Transactional(readOnly = true)
    fun getSenderPhoneForSending(): String {
        return getFreshSenderPhone().ifBlank {
            throw InternalServerErrorException("동아리 공식 발신번호가 설정되지 않았습니다")
        }
    }

    private fun getFreshSenderPhone(): String = systemConfigRegistry.getFreshValue(ConfigKey.SMS_SENDER_PHONE)
}
