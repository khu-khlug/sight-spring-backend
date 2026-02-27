package com.sight.service

import com.github.f4b6a3.ulid.UlidCreator
import com.sight.controllers.http.dto.CreateTransactionRequest
import com.sight.core.exception.NotFoundException
import com.sight.core.exception.UnprocessableEntityException
import com.sight.domain.finance.Transaction
import com.sight.domain.finance.TransactionType
import com.sight.domain.notification.NotificationCategory
import com.sight.repository.TransactionRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

data class TransactionListResult(
    val count: Long,
    val transactions: List<Transaction>,
)

@Service
class TransactionService(
    private val transactionRepository: TransactionRepository,
    private val notificationService: NotificationService,
) {
    @Transactional(readOnly = true)
    fun getCurrentCumulative(): Long {
        val latestTransaction =
            transactionRepository.findLatest()
                ?: throw NotFoundException("No transaction exists")

        return latestTransaction.cumulative
    }

    @Transactional(readOnly = true)
    fun listTransactions(
        year: Int,
        offset: Int,
        limit: Int,
    ): TransactionListResult {
        val pageNumber = offset / limit
        val pageable = PageRequest.of(pageNumber, limit)
        val startDate = LocalDate.of(year, 1, 1)
        val endDate = LocalDate.of(year + 1, 1, 1)
        val page: Page<Transaction> =
            transactionRepository.findByUsedAtBetween(startDate, endDate, pageable)

        return TransactionListResult(
            count = page.totalElements,
            transactions = page.content,
        )
    }

    @Transactional
    fun deleteTransaction(id: String) {
        val transaction =
            transactionRepository.findById(id).orElseThrow {
                NotFoundException("해당 장부 내역을 찾을 수 없습니다")
            }

        transactionRepository.delete(transaction)

        // 삭제하려는 장부 내역의 바로 이전 항목의 총합
        val baseCumulative =
            transactionRepository.findPredecessor(transaction.usedAt, transaction.createdAt)?.cumulative ?: 0L

        // 삭제하려는 장부 내역 이후의 모든 항목들
        val successors = transactionRepository.findAfter(transaction.usedAt, transaction.createdAt)
        if (successors.isNotEmpty()) {
            var runningCumulative = baseCumulative
            val updated =
                successors.map { successor ->
                    runningCumulative =
                        when (successor.type) {
                            TransactionType.INCOME -> runningCumulative + successor.total
                            TransactionType.EXPENSE -> runningCumulative - successor.total
                        }
                    successor.copy(cumulative = runningCumulative)
                }
            transactionRepository.saveAll(updated)
        }

        val formattedTotal = "%,d".format(transaction.total)
        val content =
            when (transaction.type) {
                TransactionType.INCOME -> "[동아리비] ${transaction.item} 항목에서 얻은 ${formattedTotal}원이 삭제되었습니다."
                TransactionType.EXPENSE -> "[동아리비] ${transaction.item} 항목에 사용한 ${formattedTotal}원이 삭제되었습니다."
            }
        notificationService.createNotificationForManagers(
            category = NotificationCategory.SYSTEM,
            title = "장부 알림",
            content = content,
        )
    }

    @Transactional
    fun createTransaction(
        request: CreateTransactionRequest,
        authorId: Long,
    ): Transaction {
        val today = LocalDate.now()
        if (request.usedAt.isAfter(today)) {
            throw UnprocessableEntityException("미래 날짜는 입력할 수 없습니다")
        }

        val total = request.price * request.quantity

        // 추가하려는 날짜이거나 이보다 이전의 장부 내역 조회
        val prevCumulative = transactionRepository.findLatestOnOrBefore(request.usedAt)?.cumulative ?: 0L
        val cumulative =
            when (request.type) {
                TransactionType.INCOME -> prevCumulative + total
                TransactionType.EXPENSE -> prevCumulative - total
            }

        val transaction =
            Transaction(
                id = UlidCreator.getUlid().toString(),
                author = authorId,
                type = request.type,
                item = request.item,
                price = request.price,
                quantity = request.quantity,
                total = total,
                cumulative = cumulative,
                place = request.place,
                note = request.note,
                usedAt = request.usedAt,
            )

        val saved = transactionRepository.save(transaction)

        // 추가하려는 날짜 위치보다 미래 날짜에 있는 모든 항목들
        // 동일 `usedAt` 값 기준으로 항상 지금 생성되는 장부 내역의 `createdAt`이 가장 클 것이므로 미래 날짜만 조회하면 됨.
        val successors = transactionRepository.findAfterDate(request.usedAt)
        if (successors.isNotEmpty()) {
            var runningCumulative = cumulative
            val updated =
                successors.map { successor ->
                    runningCumulative =
                        when (successor.type) {
                            TransactionType.INCOME -> runningCumulative + successor.total
                            TransactionType.EXPENSE -> runningCumulative - successor.total
                        }
                    successor.copy(cumulative = runningCumulative)
                }
            transactionRepository.saveAll(updated)
        }

        val formattedTotal = "%,d".format(saved.total)
        val content =
            when (saved.type) {
                TransactionType.INCOME -> "[동아리비] ${saved.item} 항목으로 ${formattedTotal}원을 얻었습니다."
                TransactionType.EXPENSE -> "[동아리비] ${saved.item} 항목으로 ${formattedTotal}원을 사용했습니다."
            }
        notificationService.createNotificationForManagers(
            category = NotificationCategory.SYSTEM,
            title = "장부 알림",
            content = content,
        )

        return saved
    }
}
