package solutions.cvs.sdk

/// Base SDK error
open class SDKError(
    message: String?,
    cause: Throwable? = null,
    enableSuppression: Boolean = true,
    writableStackTrace: Boolean = true
) :
    Exception(message, cause, enableSuppression, writableStackTrace)