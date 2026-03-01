package com.sight.util

import java.time.LocalDate

data class UnivTerm(val year: Int, val semester: Int) {
    fun next(): UnivTerm {
        return if (semester == 2) {
            UnivTerm(year + 1, 1)
        } else {
            UnivTerm(year, 2)
        }
    }

    fun isAfter(other: UnivTerm): Boolean {
        return year > other.year || (year == other.year && semester > other.semester)
    }

    fun isSame(other: UnivTerm): Boolean {
        return year == other.year && semester == other.semester
    }
}

enum class UnivPeriodType {
    /** 1학기 개강일 ~ 1학기 중간고사 종료 기준일 */
    FIRST_SEMESTER_MIDTERM_EXAM,

    /** 1학기 중간고사 종료 기준일의 익일 ~ 1학기 종강일 */
    FIRST_SEMESTER_FINAL_EXAM,

    /** 1학기 종강일의 익일 ~ 1학기 종료일 */
    SUMMER_VACATION,

    /** 2학기 개강일 ~ 2학기 중간고사 종료 기준일 */
    SECOND_SEMESTER_MIDTERM_EXAM,

    /** 2학기 중간고사 종료 기준일의 익일 ~ 2학기 종강일 */
    SECOND_SEMESTER_FINAL_EXAM,

    /** 2학기 종강일의 익일 ~ 2학기 종료일 */
    WINTER_VACATION,
}

class UnivPeriod(val year: Int, val type: UnivPeriodType) {
    fun inVacation(): Boolean {
        return type == UnivPeriodType.SUMMER_VACATION || type == UnivPeriodType.WINTER_VACATION
    }

    fun toTerm(): UnivTerm {
        return when (type) {
            UnivPeriodType.FIRST_SEMESTER_MIDTERM_EXAM,
            UnivPeriodType.FIRST_SEMESTER_FINAL_EXAM,
            UnivPeriodType.SUMMER_VACATION,
            -> UnivTerm(year, 1)

            UnivPeriodType.SECOND_SEMESTER_MIDTERM_EXAM,
            UnivPeriodType.SECOND_SEMESTER_FINAL_EXAM,
            UnivPeriodType.WINTER_VACATION,
            -> UnivTerm(year, 2)
        }
    }

    companion object {
        /**
         * @see 회비에 관한 세부 회칙 제8조
         * refDate는 KST 기준 LocalDate로 전달해야 합니다.
         */
        fun fromDate(refDate: LocalDate): UnivPeriod {
            val year = refDate.year

            // 1학기 기준일 계산
            val firstStart = LocalDate.of(year, 3, 1)
            val firstMidTermEnd = firstStart.plusWeeks(8).minusDays(1)
            val firstFinalEnd = firstStart.plusWeeks(16).minusDays(1)
            val firstEnd = LocalDate.of(year, 9, 1).minusDays(1)

            // 2학기 기준일 계산
            val secondStart = LocalDate.of(year, 9, 1)
            val secondMidTermEnd = secondStart.plusWeeks(8).minusDays(1)
            val secondFinalEnd = secondStart.plusWeeks(16).minusDays(1)

            return when {
                refDate.isBefore(firstStart) ->
                    UnivPeriod(year - 1, UnivPeriodType.WINTER_VACATION)

                !refDate.isAfter(firstMidTermEnd) ->
                    UnivPeriod(year, UnivPeriodType.FIRST_SEMESTER_MIDTERM_EXAM)

                !refDate.isAfter(firstFinalEnd) ->
                    UnivPeriod(year, UnivPeriodType.FIRST_SEMESTER_FINAL_EXAM)

                !refDate.isAfter(firstEnd) ->
                    UnivPeriod(year, UnivPeriodType.SUMMER_VACATION)

                !refDate.isAfter(secondMidTermEnd) ->
                    UnivPeriod(year, UnivPeriodType.SECOND_SEMESTER_MIDTERM_EXAM)

                !refDate.isAfter(secondFinalEnd) ->
                    UnivPeriod(year, UnivPeriodType.SECOND_SEMESTER_FINAL_EXAM)

                else ->
                    UnivPeriod(year, UnivPeriodType.WINTER_VACATION)
            }
        }
    }
}
