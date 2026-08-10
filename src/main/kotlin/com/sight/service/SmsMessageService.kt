package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.core.exception.InternalServerErrorException
import com.sight.domain.member.StudentStatus
import com.sight.domain.member.UserStatus
import com.sight.domain.sms.PhoneNumberNormalizer
import com.sight.domain.sms.SmsMessage
import com.sight.domain.sms.SmsMessageType
import com.sight.repository.MemberRepository
import com.sight.service.sms.SendSmsMessage
import com.sight.service.sms.SmsMessageClient
import com.sight.service.sms.SmsMessageClientException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

enum class SmsMessageResultStatus {
    SENT,
    FAILED,
    SKIPPED,
}

data class SmsMessageResult(
    val memberId: Long?,
    val phone: String?,
    val type: SmsMessageType?,
    val status: SmsMessageResultStatus,
    val message: String?,
)

data class CreateSmsMessagesResult(
    val results: List<SmsMessageResult>,
) {
    val hasFailures: Boolean = results.any { it.status != SmsMessageResultStatus.SENT }
}

@Service
class SmsMessageService(
    private val memberRepository: MemberRepository,
    private val senderPhoneService: SenderPhoneService,
    private val smsMessageClient: SmsMessageClient,
) {
    @Transactional(readOnly = true)
    fun createSmsMessages(
        memberIds: List<Long>,
        additionalPhoneNumbers: List<String>,
        message: String,
    ): CreateSmsMessagesResult {
        if (message.isBlank()) {
            throw BadRequestException("문자 메시지 내용은 공백일 수 없습니다")
        }

        val uniqueMemberIds = memberIds.distinct()
        val membersById = memberRepository.findAllById(uniqueMemberIds).associateBy { it.id }
        val hasInvalidMember =
            uniqueMemberIds.any { memberId ->
                val member = membersById[memberId]
                member == null || member.status == UserStatus.UNAUTHORIZED || member.studentStatus == StudentStatus.UNITED
            }
        if (hasInvalidMember) {
            throw BadRequestException("문자 수신자로 지정할 수 없는 회원이 포함되어 있습니다")
        }

        val plans = mutableListOf<RecipientPlan>()
        val usedPhones = mutableSetOf<String>()
        uniqueMemberIds.forEach { memberId ->
            val member = checkNotNull(membersById[memberId])
            val phone = PhoneNumberNormalizer.normalize(member.phone)
            if (phone.isBlank()) {
                plans += RecipientPlan.skipped(memberId)
            } else if (usedPhones.add(phone)) {
                plans +=
                    RecipientPlan.sendable(
                        memberId = memberId,
                        phone = phone,
                        message = SmsMessage.personalize(message, member.realname),
                    )
            }
        }

        additionalPhoneNumbers
            .flatMap { it.split(',') }
            .map(PhoneNumberNormalizer::normalize)
            .filter { it.isNotBlank() }
            .forEach { phone ->
                if (usedPhones.add(phone)) {
                    plans +=
                        RecipientPlan.sendable(
                            memberId = null,
                            phone = phone,
                            message = SmsMessage.personalize(message, phone),
                        )
                }
            }

        if (uniqueMemberIds.isEmpty() && plans.isEmpty()) {
            throw BadRequestException("문자 메시지를 받을 수신자가 없습니다")
        }

        val sendablePlans = plans.filter { it.message != null }
        if (sendablePlans.isEmpty()) {
            return CreateSmsMessagesResult(plans.map { it.toSkippedResult() })
        }

        val senderPhone = senderPhoneService.getSenderPhoneForSending()
        val indexedPlans = sendablePlans.mapIndexed { index, plan -> index.toString() to plan }
        val clientResults =
            try {
                smsMessageClient.send(
                    indexedPlans.map { (recipientIndex, plan) ->
                        SendSmsMessage(
                            recipientIndex = recipientIndex,
                            from = senderPhone,
                            to = checkNotNull(plan.phone),
                            text = checkNotNull(plan.message).text,
                            type = plan.message.type,
                        )
                    },
                )
            } catch (e: SmsMessageClientException) {
                throw InternalServerErrorException(e.message ?: "문자 발송 서비스 요청에 실패했습니다")
            }
        val clientResultsByIndex = clientResults.associateBy { it.recipientIndex }
        val acceptedByPlan =
            indexedPlans.associate { (recipientIndex, plan) ->
                plan to checkNotNull(clientResultsByIndex[recipientIndex]).accepted
            }

        return CreateSmsMessagesResult(
            plans.map { plan ->
                if (plan.message == null) {
                    plan.toSkippedResult()
                } else {
                    plan.toDeliveryResult(accepted = checkNotNull(acceptedByPlan[plan]))
                }
            },
        )
    }

    private data class RecipientPlan(
        val memberId: Long?,
        val phone: String?,
        val message: SmsMessage?,
    ) {
        fun toSkippedResult(): SmsMessageResult =
            SmsMessageResult(
                memberId = memberId,
                phone = null,
                type = null,
                status = SmsMessageResultStatus.SKIPPED,
                message = "회원에게 등록된 전화번호가 없어 발송하지 않았습니다",
            )

        fun toDeliveryResult(accepted: Boolean): SmsMessageResult =
            SmsMessageResult(
                memberId = memberId,
                phone = phone,
                type = message?.type,
                status = if (accepted) SmsMessageResultStatus.SENT else SmsMessageResultStatus.FAILED,
                message = if (accepted) null else "문자 발송 서비스가 수신자를 거부했습니다",
            )

        companion object {
            fun skipped(memberId: Long): RecipientPlan = RecipientPlan(memberId = memberId, phone = null, message = null)

            fun sendable(
                memberId: Long?,
                phone: String,
                message: SmsMessage,
            ): RecipientPlan = RecipientPlan(memberId = memberId, phone = phone, message = message)
        }
    }
}
