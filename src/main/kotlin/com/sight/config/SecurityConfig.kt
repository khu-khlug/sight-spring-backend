package com.sight.config

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
        internalApiAuthenticationFilter: InternalApiAuthenticationFilter,
        cookieAuthenticationFilter: CookieAuthenticationFilter?,
        mockAuthenticationFilter: MockAuthenticationFilter?,
        customAuthenticationEntryPoint: CustomAuthenticationEntryPoint,
        customAccessDeniedHandler: CustomAccessDeniedHandler,
    ): SecurityFilterChain {
        val httpSecurity =
            http
                .csrf { it.disable() }
                .cors { }
                .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
                .authorizeHttpRequests { auth ->
                    auth
                        .requestMatchers("/ping", "/actuator/**", "/test/public").permitAll()
                        .anyRequest().authenticated()
                }
                .exceptionHandling { exceptions ->
                    exceptions
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler)
                }
                .addFilterBefore(internalApiAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        cookieAuthenticationFilter?.let { filter ->
            httpSecurity.addFilterAfter(filter, InternalApiAuthenticationFilter::class.java)
        }

        mockAuthenticationFilter?.let { filter ->
            httpSecurity.addFilterAfter(filter, InternalApiAuthenticationFilter::class.java)
        }

        return httpSecurity.build()
    }
}
