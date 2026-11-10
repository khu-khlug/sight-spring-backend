package com.sight.core.id

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import kotlin.reflect.KClass

// 정수 식별자를 담는 문자열이 선행 0 없는 양의 Long 10진 표기인지 검증한다.
// TSID는 JavaScript의 안전한 정수 범위를 넘으므로 API에서는 정수 식별자를 문자열로 주고받는다.
// 값이 없는 경우는 다른 제약이 판단한다.
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [IntegerIdValidator::class])
annotation class IntegerId(
    val message: String = "식별자는 양의 정수 문자열이어야 합니다.",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

class IntegerIdValidator : ConstraintValidator<IntegerId, String> {
    override fun isValid(
        value: String?,
        context: ConstraintValidatorContext,
    ): Boolean = value == null || (INTEGER_ID_PATTERN.matches(value) && value.toLongOrNull() != null)

    companion object {
        private val INTEGER_ID_PATTERN = Regex("[1-9][0-9]{0,18}")
    }
}
