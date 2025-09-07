package com.sight.config

import com.sight.core.auth.AuthService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.client.RestTemplate

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@EnableAspectJAutoProxy
class SecurityConfig {
    @Bean
    fun restTemplate(): RestTemplate {
        return RestTemplate()
    }

    @Bean
    fun filterChain(
        http: HttpSecurity,
        authService: AuthService,
    ): SecurityFilterChain {
        val cookieAuthenticationFilter = CookieAuthenticationFilter(authService)

        return http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/ping", "/actuator/**", "/test/public").permitAll()
                    .anyRequest().authenticated()
            }
            .addFilterBefore(cookieAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .build()
    }
}
