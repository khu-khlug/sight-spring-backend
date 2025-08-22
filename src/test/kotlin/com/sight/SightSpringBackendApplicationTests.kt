package com.sight

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class SightSpringBackendApplicationTests {
    @Test
    fun `기본적인 테스트 - 1은 1과 같다`() {
        assert(1 == 1)
    }

    @Test
    fun `컨텍스트가 로드된다`() {
        // 이 테스트는 Spring 컨텍스트가 정상적으로 로드되는지 확인합니다
    }
}
