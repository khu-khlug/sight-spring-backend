package com.sight.core.discord

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service

/**
 * Discord 이벤트 리스너들을 자동으로 발견하고 등록하는 서비스
 * NestJS의 DiscoveryService와 유사한 역할을 수행합니다.
 */
@Service
class DiscordEventListenerRegistry(
    private val applicationContext: ApplicationContext,
    @Autowired(required = false) private val jda: JDA?,
) {
    private val logger = LoggerFactory.getLogger(DiscordEventListenerRegistry::class.java)

    /**
     * Spring 컨텍스트가 완전히 초기화된 후 Discord 이벤트 리스너들을 자동 발견하고 등록합니다.
     */
    @EventListener(ContextRefreshedEvent::class)
    fun registerDiscordEventListeners() {
        if (jda == null) {
            logger.warn("DISCORD_ENABLED=false로 설정되어 Discord 이벤트 리스너를 등록하지 않습니다.")
            return
        }

        logger.info("Discord 이벤트 리스너 자동 등록을 시작합니다.")

        val listenerBeans = applicationContext.getBeansWithAnnotation(DiscordEventListener::class.java)

        listenerBeans.forEach { (beanName, bean) ->
            if (bean is ListenerAdapter) {
                jda.addEventListener(bean)
                logger.info("Discord 이벤트 리스너 등록 완료: {} ({})", beanName, bean::class.simpleName)
            } else {
                logger.warn(
                    "@DiscordEventListener가 붙은 빈이 ListenerAdapter를 상속하지 않습니다: {} ({})",
                    beanName,
                    bean::class.simpleName,
                )
            }
        }

        logger.info(
            "총 {}개의 Discord 이벤트 리스너가 등록되었습니다.",
            listenerBeans.filter { it.value is ListenerAdapter }.size,
        )
    }
}
