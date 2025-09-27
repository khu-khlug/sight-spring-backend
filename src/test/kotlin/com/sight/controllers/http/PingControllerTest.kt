package com.sight.controllers.http

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(PingController::class, excludeAutoConfiguration = [SecurityAutoConfiguration::class])
class PingControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `ping API는 pong을 반환한다`() {
        mockMvc.perform(get("/ping"))
            .andExpect(status().isOk)
            .andExpect(content().string("pong"))
    }
}
