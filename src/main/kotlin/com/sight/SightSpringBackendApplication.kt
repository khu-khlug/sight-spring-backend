package com.sight

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SightSpringBackendApplication

fun main(args: Array<String>) {
    runApplication<SightSpringBackendApplication>(*args)
}
