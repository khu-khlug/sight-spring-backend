package com.sight.controllers.http

import com.fasterxml.jackson.databind.node.NullNode
import com.sight.controllers.http.dto.GetSeminarResponse
import com.sight.core.auth.Auth
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.service.SeminarService
import org.springframework.http.HttpStatus
import org.springframework.http.converter.json.MappingJacksonValue
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
class SeminarController(private val seminarService: SeminarService) {
    @Auth([UserRole.USER, UserRole.MANAGER])
    @GetMapping("/seminars/current")
    @ResponseStatus(HttpStatus.OK)
    fun getOpenSeminar(requester: Requester): MappingJacksonValue {
        val seminar = seminarService.getOpenSeminar(Instant.now()) ?: return MappingJacksonValue(NullNode.instance)
        return MappingJacksonValue(GetSeminarResponse.from(seminar))
    }
}
