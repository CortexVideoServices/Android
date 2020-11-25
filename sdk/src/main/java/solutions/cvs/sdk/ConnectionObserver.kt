package solutions.cvs.sdk

import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import solutions.cvs.sdk.Connection
import java.io.PrintWriter
import java.io.StringWriter


/// Connection observer
open class ConnectionObserver(
    private var onConnected: (() -> Unit)? = null,
    private var onDisconnected: (() -> Unit)? = null,
    private val onMessage: ((message: JSONObject) -> Unit)? = null,
    private var onConnectionError: ((reason: Throwable) -> Unit)? = null,
    private var onError: ((reason: Throwable) -> Unit)? = null,
    var logger: ((message: String) -> Unit)? = null
) : Connection.Observer {

    private val mainThread = Handler(Looper.getMainLooper())

    constructor(logger: (message: String) -> Unit) : this(null, null, null, null, null, logger)

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

    /// Called when a message received
    override fun onMessage(message: JSONObject) {
        mainThread.post {
            logger?.invoke("${this}.onMessage: $message")
            onMessage?.invoke(message)
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
}