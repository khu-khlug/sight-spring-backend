package com.sight.core.config

import com.sight.core.exception.InvalidConfigValueException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

class SystemConfigRegistryTest {
    private val systemConfigRepository = mock<SystemConfigRepository>()
    private lateinit var systemConfigRegistry: SystemConfigRegistry

    @BeforeEach
    fun setUp() {
        systemConfigRegistry = SystemConfigRegistry(systemConfigRepository)
    }

    @Test
    fun `getValue는 DB에 값이 있으면 DB 값을 반환한다`() {
        // given
        val key = ConfigKey.KHLUG_ACCOUNT_NUMBER
        val dbConfig =
            SystemConfig(
                id = "01HZ9QXXX0000000000000001",
                configKey = key,
                configValue = "true",
            )
        whenever(systemConfigRepository.findByConfigKey(key)).thenReturn(dbConfig)

        // when
        val result = systemConfigRegistry.getValue(key)

        // then
        assertEquals("true", result)
        verify(systemConfigRepository).findByConfigKey(key)
    }

    @Test
    fun `getValue는 DB에 값이 없으면 기본값을 반환한다`() {
        // given
        val key = ConfigKey.KHLUG_ACCOUNT_NUMBER
        whenever(systemConfigRepository.findByConfigKey(key)).thenReturn(null)

        // when
        val result = systemConfigRegistry.getValue(key)

        // then
        assertEquals(key.defaultValue, result)
        verify(systemConfigRepository).findByConfigKey(key)
    }

    @Test
    fun `getValue는 캐시에서 값을 조회하면 DB를 조회하지 않는다`() {
        // given
        val key = ConfigKey.KHLUG_ACCOUNT_NUMBER
        val dbConfig =
            SystemConfig(
                id = "01HZ9QXXX0000000000000002",
                configKey = key,
                configValue = "20",
            )
        whenever(systemConfigRepository.findByConfigKey(key)).thenReturn(dbConfig)

        // when - 첫 번째 조회 (캐시 미스)
        systemConfigRegistry.getValue(key)

        // when - 두 번째 조회 (캐시 히트)
        val result = systemConfigRegistry.getValue(key)

        // then
        assertEquals("20", result)
        verify(systemConfigRepository).findByConfigKey(key) // DB는 한 번만 조회
    }

    @Test
    fun `getValueAsBoolean은 Boolean 값으로 반환한다`() {
        // given
        val key = ConfigKey.KHLUG_ACCOUNT_NUMBER
        val dbConfig =
            SystemConfig(
                id = "01HZ9QXXX0000000000000003",
                configKey = key,
                configValue = "true",
            )
        whenever(systemConfigRepository.findByConfigKey(key)).thenReturn(dbConfig)

        // when
        val result = systemConfigRegistry.getValueAsBoolean(key)

        // then
        assertEquals(true, result)
    }

    @Test
    fun `getValueAsInt는 Int 값으로 반환한다`() {
        // given
        val key = ConfigKey.KHLUG_ACCOUNT_NUMBER
        val dbConfig =
            SystemConfig(
                id = "01HZ9QXXX0000000000000004",
                configKey = key,
                configValue = "15",
            )
        whenever(systemConfigRepository.findByConfigKey(key)).thenReturn(dbConfig)

        // when
        val result = systemConfigRegistry.getValueAsInt(key)

        // then
        assertEquals(15, result)
    }

    @Test
    fun `getValueAsInt는 정수로 파싱할 수 없으면 InvalidConfigValueException을 던진다`() {
        // given
        val key = ConfigKey.KHLUG_ACCOUNT_NUMBER
        val dbConfig =
            SystemConfig(
                id = "01HZ9QXXX0000000000000004",
                configKey = key,
                configValue = "invalid",
            )
        whenever(systemConfigRepository.findByConfigKey(key)).thenReturn(dbConfig)

        // when & then
        assertThrows<InvalidConfigValueException> {
            systemConfigRegistry.getValueAsInt(key)
        }
    }

    @Test
    fun `getValueAsLong은 Long 값으로 반환한다`() {
        // given
        val key = ConfigKey.KHLUG_ACCOUNT_NUMBER
        val dbConfig =
            SystemConfig(
                id = "01HZ9QXXX0000000000000005",
                configKey = key,
                configValue = "60",
            )
        whenever(systemConfigRepository.findByConfigKey(key)).thenReturn(dbConfig)

        // when
        val result = systemConfigRegistry.getValueAsLong(key)

        // then
        assertEquals(60L, result)
    }

    @Test
    fun `getValueAsLong은 Long으로 파싱할 수 없으면 InvalidConfigValueException을 던진다`() {
        // given
        val key = ConfigKey.KHLUG_ACCOUNT_NUMBER
        val dbConfig =
            SystemConfig(
                id = "01HZ9QXXX0000000000000005",
                configKey = key,
                configValue = "invalid",
            )
        whenever(systemConfigRepository.findByConfigKey(key)).thenReturn(dbConfig)

        // when & then
        assertThrows<InvalidConfigValueException> {
            systemConfigRegistry.getValueAsLong(key)
        }
    }

    @Test
    fun `setValue는 기존 설정이 있으면 업데이트한다`() {
        // given
        val key = ConfigKey.KHLUG_ACCOUNT_NUMBER
        val existingConfig =
            SystemConfig(
                id = "01HZ9QXXX0000000000000006",
                configKey = key,
                configValue = "false",
            )
        val updatedConfig = existingConfig.copy(configValue = "true")
        whenever(systemConfigRepository.findByConfigKey(key)).thenReturn(existingConfig)
        whenever(systemConfigRepository.save(any<SystemConfig>())).thenReturn(updatedConfig)

        // when
        val result = systemConfigRegistry.setValue(key, "true")

        // then
        assertEquals("true", result.configValue)
        verify(systemConfigRepository).findByConfigKey(key)
        verify(systemConfigRepository).save(any<SystemConfig>())
    }

    @Test
    fun `setValue는 기존 설정이 없으면 새로 생성한다`() {
        // given
        val key = ConfigKey.KHLUG_ACCOUNT_NUMBER
        val newConfig =
            SystemConfig(
                id = "01HZ9QXXX0000000000000007",
                configKey = key,
                configValue = "20",
            )
        whenever(systemConfigRepository.findByConfigKey(key)).thenReturn(null)
        whenever(systemConfigRepository.save(any<SystemConfig>())).thenReturn(newConfig)

        // when
        val result = systemConfigRegistry.setValue(key, "20")

        // then
        assertEquals(key, result.configKey)
        assertEquals("20", result.configValue)
        verify(systemConfigRepository).findByConfigKey(key)
        verify(systemConfigRepository).save(any<SystemConfig>())
    }

    @Test
    fun `setValue는 캐시를 갱신한다`() {
        // given
        val key = ConfigKey.KHLUG_ACCOUNT_NUMBER
        val dbConfig =
            SystemConfig(
                id = "01HZ9QXXX0000000000000008",
                configKey = key,
                configValue = "false",
            )
        val updatedConfig = dbConfig.copy(configValue = "true")
        whenever(systemConfigRepository.findByConfigKey(key)).thenReturn(dbConfig)
        whenever(systemConfigRepository.save(any<SystemConfig>())).thenReturn(updatedConfig)

        // when
        systemConfigRegistry.setValue(key, "true")

        // then - 캐시에서 조회하면 DB를 조회하지 않음
        val cachedValue = systemConfigRegistry.getValue(key)
        assertEquals("true", cachedValue)
    }

    @Test
    fun `refreshCache는 특정 키의 캐시를 무효화한다`() {
        // given
        val key = ConfigKey.KHLUG_ACCOUNT_NUMBER
        val initialConfig =
            SystemConfig(
                id = "01HZ9QXXX0000000000000009",
                configKey = key,
                configValue = "10",
            )
        val updatedConfig = initialConfig.copy(configValue = "20")
        whenever(systemConfigRepository.findByConfigKey(key))
            .thenReturn(initialConfig)
            .thenReturn(updatedConfig)

        // when - 첫 번째 조회
        systemConfigRegistry.getValue(key)

        // when - 캐시 무효화
        systemConfigRegistry.refreshCache(key)

        // when - 두 번째 조회 (캐시 무효화 후)
        val result = systemConfigRegistry.getValue(key)

        // then - DB에서 다시 조회되어 새로운 값 반환
        assertEquals("20", result)
    }

    @Test
    fun `refreshAllCache는 전체 캐시를 무효화한다`() {
        // given
        val key1 = ConfigKey.KHLUG_ACCOUNT_NUMBER
        val key2 = ConfigKey.KHLUG_ACCOUNT_NUMBER
        val config1 =
            SystemConfig(
                id = "01HZ9QXXX0000000000000010",
                configKey = key1,
                configValue = "false",
            )
        val config2 =
            SystemConfig(
                id = "01HZ9QXXX0000000000000011",
                configKey = key2,
                configValue = "10",
            )
        whenever(systemConfigRepository.findByConfigKey(key1)).thenReturn(config1)
        whenever(systemConfigRepository.findByConfigKey(key2)).thenReturn(config2)

        // when - 첫 번째 조회 (캐시 적재)
        systemConfigRegistry.getValue(key1)
        systemConfigRegistry.getValue(key2)

        // when - 전체 캐시 무효화
        systemConfigRegistry.refreshAllCache()

        // when - 두 번째 조회 (캐시 무효화 후)
        systemConfigRegistry.getValue(key1)
        systemConfigRegistry.getValue(key2)

        // then - DB에서 각각 2번씩 조회됨
        verify(systemConfigRepository, org.mockito.kotlin.times(2)).findByConfigKey(key1)
        verify(systemConfigRepository, org.mockito.kotlin.times(2)).findByConfigKey(key2)
    }
}
