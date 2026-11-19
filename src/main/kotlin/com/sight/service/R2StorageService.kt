package com.sight.service

import com.sight.domain.file.FileUpload
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.ContentDisposition
import org.springframework.stereotype.Service
import software.amazon.awssdk.core.exception.SdkException
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.HeadObjectResponse
import software.amazon.awssdk.services.s3.model.S3Exception
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest
import java.nio.charset.StandardCharsets.UTF_8

@Service
@ConditionalOnProperty(prefix = "storage.r2", name = ["enabled"], havingValue = "true")
class R2StorageService(
    private val s3: S3Client,
    private val presigner: S3Presigner,
    @Value("\${storage.r2.bucket}") private val bucket: String,
) : StorageService {
    init {
        require(bucket.isNotBlank()) { "R2_BUCKET을 설정해야 합니다." }
    }

    override fun generateUploadUrl(key: String): String = presignUpload(key, null)

    override fun generateUploadUrl(
        key: String,
        size: Long,
    ): String = presignUpload(key, size)

    private fun presignUpload(
        key: String,
        size: Long?,
    ): String =
        storageRequest {
            presigner.presignPutObject { request ->
                request.signatureDuration(FileUpload.UPLOAD_LINK_VALIDITY)
                    .putObjectRequest { it.bucket(bucket).key(key).contentLength(size) }
            }.url().toExternalForm()
        }

    override fun isFileExists(key: String): Boolean = headObject(key) != null

    override fun getFileSize(key: String): Long? {
        val objectInfo = headObject(key) ?: return null
        return objectInfo.contentLength()?.takeIf { it >= 0 }
            ?: throw StorageOperationException(StorageFailureReason.TRANSIENT, "파일 크기를 확인할 수 없습니다.")
    }

    private fun headObject(key: String): HeadObjectResponse? {
        try {
            return storageRequest { s3.headObject { it.bucket(bucket).key(key) } }
        } catch (e: StorageOperationException) {
            if (e.reason == StorageFailureReason.NO_SUCH_KEY) return null
            if (e.reason != StorageFailureReason.TRANSIENT || (e.cause as? S3Exception)?.statusCode() != 404) throw e

            // HEAD 응답에 오류 코드가 없을 수 있으므로 버킷이 유효한지 확인한다.
            try {
                storageRequest { s3.headBucket { it.bucket(bucket) } }
            } catch (bucketError: StorageOperationException) {
                if ((bucketError.cause as? S3Exception)?.statusCode() == 404) {
                    throw StorageOperationException(
                        StorageFailureReason.NO_SUCH_BUCKET,
                        "파일 저장소 버킷을 찾을 수 없습니다.",
                        bucketError,
                    )
                }
                throw bucketError
            }
            return null
        }
    }

    override fun deleteFile(key: String) {
        storageRequest { s3.deleteObject { it.bucket(bucket).key(key) } }
    }

    override fun getDownloadUrl(key: String): String = presignDownload(key, null).url().toExternalForm()

    override fun getDownloadUrl(
        key: String,
        options: FileDownloadOptions,
    ): SignedFileUrl {
        val signed = presignDownload(key, options)
        return SignedFileUrl(signed.url().toExternalForm(), signed.expiration())
    }

    private fun presignDownload(
        key: String,
        options: FileDownloadOptions?,
    ): PresignedGetObjectRequest =
        storageRequest {
            val objectRequest = GetObjectRequest.builder().bucket(bucket).key(key)
            if (options != null) {
                val disposition = if (options.inline) ContentDisposition.inline() else ContentDisposition.attachment()
                objectRequest.responseContentType(options.contentType)
                    .responseContentDisposition(disposition.filename(options.fileName, UTF_8).build().toString())
            }
            val signed =
                presigner.presignGetObject {
                    it.signatureDuration(FileUpload.DOWNLOAD_LINK_VALIDITY).getObjectRequest(objectRequest.build())
                }
            if (!signed.isBrowserExecutable) {
                throw StorageOperationException(StorageFailureReason.TRANSIENT, "브라우저에서 사용할 수 있는 파일 읽기 주소를 발급하지 못했습니다.")
            }
            signed
        }

    private fun <T> storageRequest(action: () -> T): T =
        try {
            action()
        } catch (e: S3Exception) {
            val reason =
                when {
                    e.awsErrorDetails()?.errorCode() == "NoSuchKey" -> StorageFailureReason.NO_SUCH_KEY
                    e.awsErrorDetails()?.errorCode() == "NoSuchBucket" -> StorageFailureReason.NO_SUCH_BUCKET
                    e.statusCode() in listOf(401, 403) -> StorageFailureReason.AUTHENTICATION
                    else -> StorageFailureReason.TRANSIENT
                }
            throw StorageOperationException(reason, "파일 저장소 요청에 실패했습니다.", e)
        } catch (e: SdkException) {
            throw StorageOperationException(StorageFailureReason.TRANSIENT, "파일 저장소 요청에 실패했습니다.", e)
        }
}
