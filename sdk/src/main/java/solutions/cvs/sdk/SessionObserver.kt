package solutions.cvs.sdk

import android.os.Handler
import android.os.Looper
import solutions.cvs.sdk.Session
import solutions.cvs.sdk.Stream
import java.io.PrintWriter
import java.io.StringWriter
import java.io.Writer


/// Session observer
open class SessionObserver(
    private var onError: ((reason: Throwable)-> Unit)? = null,
    private var onConnected: (()-> Unit)? = null,
    private var onConnectionError: ((reason: Throwable)-> Unit)? = null,
    private var onDisconnected: (()-> Unit)? = null,
    private var onStreamReceived: ((stream: Stream)-> Unit)? = null,
    private var onStreamDropped: ((stream: Stream)-> Unit)? = null,
    var logger: ((message: String) -> Unit)? = null
) : Session.Observer {

    private val mainThread = Handler(Looper.getMainLooper())

    constructor(logger: (message: String) -> Unit) : this(null, null, null, null, null, null, logger)

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

    /// Called when the session client connects to the session server.
    override fun onConnected() {
        mainThread.post {
            logger?.invoke("${this}.onConnected")
            onConnected?.invoke()
        }
    }

    /// Called when a connection error occurred
    override fun onConnectionError(reason: Throwable) {
        mainThread.post {
            logger?.run {
                val traceLog = StringWriter()
                reason.printStackTrace(PrintWriter(traceLog))
                invoke("${this}.onConnectionError: $reason\n$traceLog")
            }
            onConnectionError?.invoke(reason)
        }
    }

    /// Called when the session client has disconnected from the session server.
    override fun onDisconnected() {
        mainThread.post {
            logger?.invoke("${this}.onDisconnected")
            onDisconnected?.invoke()
        }
    }

    /// Called when a new stream is received published for this session.
    override fun onStreamReceived(stream: Stream) {
        mainThread.post {
            logger?.invoke("${this}.onStreamReceived: $stream")
            onStreamReceived?.invoke(stream)
        }
    }

    /// Called when stops publishing a stream to this session.
    override fun onStreamDropped(stream: Stream) {
        mainThread.post {
            logger?.invoke("${this}.onStreamDropped: $stream")
            onStreamDropped?.invoke(stream)
        }
    }
}
