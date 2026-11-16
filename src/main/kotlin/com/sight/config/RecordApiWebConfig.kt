package com.sight.config

import com.sight.core.exception.BadRequestException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.HandlerMapping
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class RecordApiWebConfig : WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(RecordPathInterceptor())
            .addPathPatterns("/groups/*/records/**", "/groups/*/cards/*/records")
    }
}

private class RecordPathInterceptor : HandlerInterceptor {
    private val decimalId = Regex("[1-9][0-9]{0,18}")

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
