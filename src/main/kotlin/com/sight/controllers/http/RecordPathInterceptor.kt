package com.sight.controllers.http

import com.sight.core.exception.BadRequestException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.HandlerMapping
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Component
class RecordPathInterceptor : HandlerInterceptor, WebMvcConfigurer {
    private val decimalId = Regex("[1-9][0-9]{0,18}")

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(this)
            .addPathPatterns("/groups/*/records/**", "/groups/*/cards/*/records")
    }

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        val variables = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE) as? Map<*, *> ?: return true
        for (name in listOf("groupId", "cardId", "recordId")) {
            val value = variables[name] as? String ?: continue
            if (!decimalId.matches(value) || value.toLongOrNull() == null) {
                throw BadRequestException("$name 식별자는 선행 0 없는 양의 Long 10진 문자열이어야 합니다.")
            }
        }
        return true
    }
}
