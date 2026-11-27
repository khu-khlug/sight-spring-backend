package com.sight.service

enum class StorageFailureReason {
    NO_SUCH_KEY,
    NO_SUCH_BUCKET,
    AUTHENTICATION,
    TRANSIENT,
}

class StorageOperationException(val reason: StorageFailureReason, message: String, cause: Throwable? = null) : RuntimeException(
    message,
    cause,
) {
    val stopsMaintenance: Boolean
        get() = reason == StorageFailureReason.NO_SUCH_BUCKET || reason == StorageFailureReason.AUTHENTICATION
}
