package com.sight.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import java.net.URI
import java.time.Duration

@Configuration
@ConditionalOnProperty(prefix = "storage.r2", name = ["enabled"], havingValue = "true")
class R2StorageConfig(
    @Value("\${storage.r2.endpoint}") endpoint: String,
    @Value("\${storage.r2.access-key-id}") accessKeyId: String,
    @Value("\${storage.r2.secret-access-key}") secretAccessKey: String,
) {
    private val endpointUri = URI(endpoint)
    private val credentials: StaticCredentialsProvider
    private val region = Region.of("auto")
    private val s3Configuration =
        S3Configuration.builder().pathStyleAccessEnabled(true).checksumValidationEnabled(false).build()

    init {
        require(endpointUri.scheme == "https" && endpointUri.host != null && endpointUri.rawUserInfo == null &&
            endpointUri.rawQuery == null && endpointUri.rawFragment == null && endpointUri.path in listOf("", "/")
        ) { "R2_ENDPOINT는 HTTPS 계정 엔드포인트여야 합니다." }
        require(accessKeyId.isNotBlank() && secretAccessKey.isNotBlank()) { "R2 접근 키를 설정해야 합니다." }
        credentials = StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId, secretAccessKey))
    }

    @Bean
    fun r2S3Client(): S3Client =
        S3Client.builder()
            .endpointOverride(endpointUri)
            .region(region)
            .credentialsProvider(credentials)
            .serviceConfiguration(s3Configuration)
            .overrideConfiguration {
                it.apiCallTimeout(Duration.ofSeconds(5)).apiCallAttemptTimeout(Duration.ofSeconds(2))
            }
            .build()

    @Bean
    fun r2S3Presigner(): S3Presigner =
        S3Presigner.builder()
            .endpointOverride(endpointUri)
            .region(region)
            .credentialsProvider(credentials)
            .serviceConfiguration(s3Configuration)
            .build()
}
