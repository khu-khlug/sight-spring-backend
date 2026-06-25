package com.sight.core.khuis

import org.springframework.stereotype.Component

@Component
class KhuisClient {
    fun authenticate(
        info21Id: String,
        info21Password: String,
    ): Boolean {
        // TODO: 실제 Info21 (KHUIS) 외부 인증 API 연동 필요
        // 임시로 항상 성공하도록 처리하거나 간이 검증을 수행합니다.
        if (info21Id.isBlank() || info21Password.isBlank()) {
            return false
        }
        return true
    }
}
