package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.core.exception.NotFoundException
import com.sight.repository.GroupCardRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

// 카드 자체에서 확인한 기본 정보만 담는다. 상세 API 응답은 후속 조회에서 구성한다.
data class GroupCardBasicResult(
    val id: Long,
    val groupId: Long,
    val title: String,
    val isDisabled: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Service
class GroupCardReadService(private val cards: GroupCardRepository) {
    @Transactional(readOnly = true)
    fun getGroupCardById(
        groupId: Long,
        cardId: Long,
    ): GroupCardBasicResult {
        // 두 조회 모두 그룹 소속을 제한해 다른 그룹 카드의 삭제 여부를 노출하지 않는다.
        val card =
            cards.findAvailableByIdAndGroupId(cardId, groupId) ?: run {
                if (cards.existsDeletedByIdAndGroupId(cardId, groupId)) throw BadRequestException("삭제된 카드입니다.")
                throw NotFoundException("카드를 찾을 수 없습니다.")
            }
        return GroupCardBasicResult(card.id, card.groupId, card.title, card.isDisabled, card.createdAt, card.updatedAt)
    }
}
