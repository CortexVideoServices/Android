package solutions.cvs.sdk

import android.os.Handler
import android.os.Looper
import java.io.PrintWriter
import java.io.StringWriter

/// Publisher observer
open class PublisherObserver(
    private var onError: ((reason: Throwable)-> Unit)? = null,
    private var onStreamCreated: ((stream: Stream)-> Unit)? = null,
    private var onStreamDestroy: ((stream: Stream)-> Unit)? = null,
    var logger: ((message: String) -> Unit)? = null
) : Publisher.Observer {

    private val mainThread = Handler(Looper.getMainLooper())

    constructor(logger: (message: String) -> Unit) : this(null, null, null, logger)

    /// Called when an error occurred
    override fun onError(reason: Throwable) {
        mainThread.post {
            logger?.run {
                val traceLog = StringWriter()
                reason.printStackTrace(PrintWriter(traceLog))
                invoke("${this}.onError: $reason\n$traceLog")
            }
            onError?.invoke(reason)
        }
    }

    /// Called when a participant's stream has been created
    override fun onStreamCreated(stream: Stream) {
        mainThread.post {
            logger?.invoke("${this}.onStreamCreated: $stream")
            onStreamCreated?.invoke(stream)
        }
    }

    /// Called before a participant's stream is destroyed
    override fun onStreamDestroy(stream: Stream) {
        mainThread.post {
            logger?.invoke("${this}.onStreamDestroy: $stream")
            onStreamDestroy?.invoke(stream)
        }
    }
}