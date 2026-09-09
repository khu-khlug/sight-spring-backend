package com.sight.controllers.http

import com.sight.service.HelloPostService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(InternalHelloPostController::class, excludeAutoConfiguration = [SecurityAutoConfiguration::class])
class InternalHelloPostControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var helloPostService: HelloPostService

    @Test
    fun `Hello 게시물 목록은 지정한 limit을 사용한다`() {
        given(helloPostService.listHelloPosts(3)).willReturn(emptyList())

        mockMvc.perform(get("/internal/hello-posts").param("limit", "3"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.posts").isEmpty)
    }

    @Test
    fun `Hello 게시물 목록의 limit은 1부터 10까지 허용한다`() {
        mockMvc.perform(get("/internal/hello-posts").param("limit", "0"))
            .andExpect(status().isBadRequest)

        mockMvc.perform(get("/internal/hello-posts").param("limit", "11"))
            .andExpect(status().isBadRequest)
    }
}
