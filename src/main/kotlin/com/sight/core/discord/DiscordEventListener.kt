package com.sight.core.discord

/**
 * Discord 이벤트 리스너로 자동 등록될 클래스에 사용하는 어노테이션
 * 이 어노테이션이 붙은 클래스는 애플리케이션 시작 시 JDA에 자동으로 등록됩니다.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class DiscordEventListener
