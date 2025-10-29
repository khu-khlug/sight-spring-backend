package com.sight.service

import com.sight.core.exception.NotFoundException
import com.sight.domain.finance.Transaction
import com.sight.repository.TransactionRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.springframework.http.HttpStatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

class TransactionServiceTest {
    private val transactionRepository = mock<TransactionRepository>()
    private lateinit var transactionService: TransactionService

    @BeforeEach
    fun setUp() {
        transactionService = TransactionService(transactionRepository)
    }

    @Test
    fun `deleteTransaction은 존재하는 거래 내역을 삭제한다`() {
        // given
        val transactionId = 1L
        val transaction =
            Transaction(
                id = transactionId,
                author = 1L,
                year = 2024,
                month = 1,
                item = "테스트 항목",
                price = 10000L,
                quantity = 1L,
                total = 10000L,
                cumulative = 10000L,
                place = "테스트 장소",
                note = "테스트 노트",
                usedAt = LocalDate.of(2024, 1, 15),
                createdAt = LocalDateTime.of(2024, 1, 15, 10, 0),
                updatedAt = LocalDateTime.of(2024, 1, 15, 10, 0),
            )
        given(transactionRepository.findById(transactionId)).willReturn(Optional.of(transaction))

        // when
        transactionService.deleteTransaction(transactionId)

        // then
        verify(transactionRepository).delete(transaction)
    }

    @Test
    fun `deleteTransaction은 존재하지 않는 거래 내역 삭제 시 NotFoundException을 던진다`() {
        // given
        val transactionId = 999L
        given(transactionRepository.findById(transactionId)).willReturn(Optional.empty())

        // when & then
        val exception =
            assertThrows<NotFoundException> {
                transactionService.deleteTransaction(transactionId)
            }
        assertEquals(HttpStatus.NOT_FOUND, exception.statusCode)
    }
}
