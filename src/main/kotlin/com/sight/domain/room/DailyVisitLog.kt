package com.sight.domain.room

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.io.Serializable
import java.time.LocalDate

data class DailyVisitLogId(
    val roomNumber: Int = 0,
    val memberId: Long = 0,
) : Serializable

@Entity
@Table(name = "daily_visit_log")
@IdClass(DailyVisitLogId::class)
class DailyVisitLog(
    roomNumber: Int,
    memberId: Long,
    date: LocalDate,
) {
    @Id
    @Column(name = "room_number", nullable = false)
    val roomNumber: Int = roomNumber

    @Id
    @Column(name = "member_id", nullable = false)
    val memberId: Long = memberId

    @Column(name = "date", nullable = false)
    var date: LocalDate = date
        private set

    fun visit(date: LocalDate) {
        this.date = date
    }
}
