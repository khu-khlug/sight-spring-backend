package com.sight.service

import com.sight.controllers.http.dto.CreateTransactionRequest
import com.sight.core.exception.NotFoundException
import com.sight.core.exception.UnprocessableEntityException
import com.sight.domain.finance.Transaction
import com.sight.domain.finance.TransactionType
import com.sight.repository.TransactionRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
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
        val transactionId = "transactionId"
        val transaction =
            Transaction(
                id = transactionId,
                author = 1L,
                type = TransactionType.EXPENSE,
                item = "테스트 항목",
                price = 10000L,
                quantity = 1L,
                total = 10000L,
                cumulative = 10000L,
                place = "테스트 수급처",
                note = "테스트 노트",
                usedAt = LocalDate.of(2024, 1, 15),
                createdAt = LocalDateTime.of(2024, 1, 15, 10, 0),
                updatedAt = LocalDateTime.of(2024, 1, 15, 10, 0),
            )
        given(transactionRepository.findById(transactionId)).willReturn(Optional.of(transaction))

        // when & then (예외가 발생하지 않으면 성공)
        transactionService.deleteTransaction(transactionId)
    }

    @Test
    fun `deleteTransaction은 존재하지 않는 거래 내역 삭제 시 NotFoundException을 던진다`() {
        // given
        val transactionId = "999"
        given(transactionRepository.findById(transactionId)).willReturn(Optional.empty())

        // when & then

        assertThrows<NotFoundException> {
            transactionService.deleteTransaction(transactionId)
        }
    }

    @Test
    fun `createTransaction은 이전 거래가 없을 때 cumulative를 total과 같게 설정한다`() {
        // given
        val authorId = 1L
        val request =
            CreateTransactionRequest(
                type = TransactionType.EXPENSE,
                item = "테스트 항목",
                price = 10000L,
                quantity = 2L,
                place = "테스트 장소",
                note = "테스트 노트",
                usedAt = LocalDate.of(2024, 1, 15),
            )

        given(transactionRepository.findLatest()).willReturn(null)
        given(transactionRepository.save(any<Transaction>())).willAnswer {
            it.arguments[0] as Transaction
        }

        // when
        val result = transactionService.createTransaction(request, authorId)

        // then
        assertEquals(20000L, result.total) // 10000 * 2
        assertEquals(20000L, result.cumulative) // 이전 거래 없음, total과 동일
    }

    @Test
    fun `createTransaction은 이전 거래가 있을 때 cumulative를 누적한다`() {
        // given
        val authorId = 1L
        val request =
            CreateTransactionRequest(
                type = TransactionType.EXPENSE,
                item = "테스트 항목",
                price = 5000L,
                quantity = 1L,
                place = null,
                note = null,
                usedAt = LocalDate.of(2024, 1, 20),
            )

        val previousTransaction =
            Transaction(
                id = "transactionId",
                author = 1L,
                type = TransactionType.EXPENSE,
                item = "이전 항목",
                price = 10000L,
                quantity = 1L,
                total = 10000L,
                cumulative = 10000L,
                place = null,
                note = null,
                usedAt = LocalDate.of(2024, 1, 15),
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
            )

        given(transactionRepository.findLatest()).willReturn(previousTransaction)
        given(transactionRepository.save(any<Transaction>())).willAnswer {
            it.arguments[0] as Transaction
        }

        // when
        val result = transactionService.createTransaction(request, authorId)

        // then
        assertEquals(5000L, result.total) // 5000 * 1
        assertEquals(15000L, result.cumulative) // 10000 + 5000
    }

    @Test
    fun `createTransaction은 미래 날짜 입력 시 예외를 던진다`() {
        // given
        val authorId = 1L
        val request =
            CreateTransactionRequest(
                type = TransactionType.EXPENSE,
                item = "테스트",
                price = 1000L,
                quantity = 1L,
                place = null,
                note = null,
                usedAt = LocalDate.now().plusDays(1),
            )

        // when & then
        assertThrows<UnprocessableEntityException> {
            transactionService.createTransaction(request, authorId)
        }
    }
}
