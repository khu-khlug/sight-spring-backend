package com.sight.core.config

import com.github.f4b6a3.ulid.UlidCreator
import com.sight.core.exception.InvalidConfigValueException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

@Service
class SystemConfigRegistry(
    private val systemConfigRepository: SystemConfigRepository,
) {
    private data class CacheEntry(
        val value: String,
        val expiresAt: Instant,
    )

    private val cache = ConcurrentHashMap<ConfigKey, CacheEntry>()

    /**
     * 설정 값을 조회합니다.
     * 캐시 → DB → 기본값 순서로 조회하며, DB에 값이 없으면 기본값을 반환합니다. (DB에 저장하지 않음)
     */
    @Transactional(readOnly = true)
    fun getValue(key: ConfigKey): String {
        // 1. 캐시에서 조회
        val cachedEntry = cache[key]
        if (cachedEntry != null && Instant.now().isBefore(cachedEntry.expiresAt)) {
            return cachedEntry.value
        }

        // 캐시 만료된 경우 제거
        if (cachedEntry != null) {
            cache.remove(key)
        }

        // 2. DB에서 조회
        val config = systemConfigRepository.findByConfigKey(key)
        return if (config != null) {
            // DB에서 찾은 값을 캐시에 저장
            val entry =
                CacheEntry(
                    value = config.configValue,
                    expiresAt = Instant.now().plusSeconds(60),
                )
            cache[key] = entry
            config.configValue
        } else {
            // DB에 없으면 기본값 반환 (DB에 저장하지 않음)
            key.defaultValue
        }
    }

    /**
     * 설정 값을 Boolean으로 파싱하여 반환합니다.
     */
    fun getValueAsBoolean(key: ConfigKey): Boolean {
        return getValue(key).toBoolean()
    }

    /**
     * 설정 값을 Int로 파싱하여 반환합니다.
     */
    fun getValueAsInt(key: ConfigKey): Int {
        return try {
            getValue(key).toInt()
        } catch (e: NumberFormatException) {
            throw InvalidConfigValueException("설정 값을 정수로 파싱할 수 없습니다. key=$key, value=${getValue(key)}")
        }
    }

    /**
     * 설정 값을 Long으로 파싱하여 반환합니다.
     */
    fun getValueAsLong(key: ConfigKey): Long {
        return try {
            getValue(key).toLong()
        } catch (e: NumberFormatException) {
            throw InvalidConfigValueException("설정 값을 Long으로 파싱할 수 없습니다. key=$key, value=${getValue(key)}")
        }
    }

    /**
     * 설정 값을 저장하고 캐시를 갱신합니다.
     * DB에 이미 존재하면 업데이트하고, 없으면 새로 생성합니다.
     */
    @Transactional
    fun setValue(
        key: ConfigKey,
        value: String,
    ): SystemConfig {
        // 캐시 무효화 먼저 수행 (동시성 이슈 방지)
        cache.remove(key)

        val existingConfig = systemConfigRepository.findByConfigKey(key)

        val savedConfig =
            if (existingConfig != null) {
                // 기존 설정 업데이트 (updatedAt 명시적 설정)
                val updatedConfig =
                    existingConfig.copy(
                        configValue = value,
                        updatedAt = LocalDateTime.now(),
                    )
                systemConfigRepository.save(updatedConfig)
            } else {
                // 새로운 설정 생성
                val newConfig =
                    SystemConfig(
                        id = UlidCreator.getUlid().toString(),
                        configKey = key,
                        configValue = value,
                    )
                systemConfigRepository.save(newConfig)
            }

        // 캐시 갱신
        val entry =
            CacheEntry(
                value = value,
                expiresAt = Instant.now().plusSeconds(60),
            )
        cache[key] = entry

        return savedConfig
    }

    /**
     * 특정 키의 캐시를 무효화합니다.
     */
    fun refreshCache(key: ConfigKey) {
        cache.remove(key)
    }

    /**
     * 전체 캐시를 무효화합니다.
     */
    fun refreshAllCache() {
        cache.clear()
    }
}
