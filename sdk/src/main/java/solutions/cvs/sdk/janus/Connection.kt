package solutions.cvs.sdk.janus

import android.util.ArraySet
import android.util.Log
import okhttp3.*
import org.json.JSONObject
import solutions.cvs.sdk.Package
import java.io.PrintWriter
import java.io.StringWriter
import java.util.*
import kotlin.concurrent.schedule

class JanusError(val code: Int, reason: String) : Exception(reason) {}
class ConnectionError(val code: Int, reason: String) : Exception(reason) {}

typealias ConnectResult = (error: Throwable?) -> Unit

///// Connection message listener
//interface RequestResult {
//    /// Invoked when the received message
//    fun onMessage(message: JSONObject): Unit
//
//    /// Invoked when the request fails.
//    fun onError(reason: Throwable): Unit
//}

typealias RequestResult = (result: Any) -> Unit

/// Connection listener
interface ConnectionListener {
    /// Invoked when the connection has created.
    fun onConnected(connection: Connection) {}

    /// Invoked when the connection has closed.
    fun onDisconnect(connection: Connection, reason: Throwable?) {}

    /// Unhandled Janus message
    fun onMessage(connection: Connection, message: JSONObject) {}

    /// Invoked when the connection fails.
    fun onError(connection: Connection, reason: Throwable) {}
}


/// Connection interface
interface Connection {

    /// URL to the media server
    val serverUrl: String

    /// Returns true if connected
    val connected: Boolean

    /// Sends message
    fun sendMessage(message: JSONObject): String?

    /// Sends request
    fun sendRequest(request: JSONObject, onResult: RequestResult)

    /// Connects to the server
    fun connect(serverUrl: String, onResult: ConnectResult? = null)

    /// Closes and destructors connection
    fun close(error: Throwable? = null)

    /// Adds connection listener
    fun addListener(listener: ConnectionListener): ConnectionListener;

    /// Removes connection listener
    fun removeListener(listener: ConnectionListener): ConnectionListener

    companion object {
        /// Creates new connection
        fun create(): Connection {
            return ConnectionImpl()
        }
    }
}


internal class ConnectionImpl(
    val requestTimeout: Long = 30,
    val keepaliveTimeout: Long = 50
) : Connection {

    private var transactionNum = 0
    private var sessionId: Long = 0
    private val listeners = ArraySet<ConnectionListener>()
    private val resultWaiters = mutableMapOf<String, Pair<RequestResult, Long>>()
    private lateinit var webSocket: WebSocket

    private fun keepalive(timeout: Long = 20) {
        // ToDo: Check requests timeout
        sendMessage(JSONObject(mapOf("janus" to "keepalive")))
        Timer("JanusKeepAlive", false).schedule(keepaliveTimeout * 1000) {
            keepalive(timeout)
        }
    }

    private fun onOpen(onResult: ConnectResult) {
        sendRequest(JSONObject(mapOf("janus" to "create"))) { result ->
            if (result is JSONObject) {
                val message = result as JSONObject
                sessionId = message.getJSONObject("data").getLong("id")
                listeners.forEach { it.onConnected(this@ConnectionImpl) }
                connected = true
                keepalive()
                onResult(null)
            } else {
                val error = Error("Cannot create connection session", if (result is Throwable) result else null)
                listeners.forEach { it.onError(this@ConnectionImpl, error) }
                onResult(error)
            }

        }
    }

    private fun onMessage(text: String) {
        val message = JSONObject(text)
        val transaction = message.optString("transaction")
        if (transaction != null && resultWaiters.containsKey(transaction)) {
            val value = resultWaiters[transaction]
            if (value != null) {
                val (onResult, _) = value
                var error = message.optJSONObject("error")
                val plugindata = message.optJSONObject("plugindata")
                if (plugindata != null) {
                    val data = plugindata.optJSONObject("data")
                    data?.run {
                        val error_code = optInt("error_code", 0)
                        if (error_code != 0) {
                            error = JSONObject(
                                mapOf(
                                    "code" to error_code,
                                    "reason" to data.optString("error", "Undefined error")
                                )
                            )
                        }
                    }
                }
                if (error != null) {
                    error?.let {
                        val code = it.getInt("code")
                        val reason = it.getString("reason")
                        resultWaiters.remove(transaction)
                        onResult(JanusError(code, reason))
                    }
                } else {
                    if (message.optJSONObject("data") != null || plugindata != null) {
                        resultWaiters.remove(transaction)
                        onResult(message)
                    }
                }
            }
        }
        listeners.forEach { listener -> listener.onMessage(this, message) }
    }

    private fun onFailure(reason: Throwable) {
        listeners.forEach { it.onError(this, Exception("Connection reset", reason)) }
    }

    private fun onClosing(code: Int, reason: String) {
        var error: Throwable = Exception("Connection reset")
        resultWaiters.values.forEach { it.first(error) }
        if (code > 3000)
            error = JanusError(code, reason)
        else if (code > 1000)
            error = ConnectionError(code, reason)
        listeners.forEach { it.onDisconnect(this, if (code > 1000) error else null) }
        connected = false
    }

    private fun onClosed() {
        resultWaiters.clear()
        listeners.clear()
    }

    init {
        if (Package.debug)
            addListener(object : ConnectionListener {
                override fun onConnected(connection: Connection) {
                    Log.d("SDK/EVENTS", "$connection; onConnected")
                }

                override fun onDisconnect(connection: Connection, reason: Throwable?) {
                    Log.d("SDK/EVENTS", "$connection; onDisconnect: $reason")
                }

                override fun onMessage(connection: Connection, message: JSONObject) {
                    Log.d("SDK/EVENTS", "$connection; onMessage: $message")
                }

                override fun onError(connection: Connection, reason: Throwable) {
                    val stackTrace = StringWriter()
                    reason.printStackTrace(PrintWriter(stackTrace))
                    Log.d("SDK/EVENTS", "$connection; onError: $reason\n$stackTrace")
                }
            })
    }

    /// URL to the media server
    override var serverUrl: String = Package.serverUrl
        private set


    /// Returns true if connected
    override var connected: Boolean = false
        private set

    /// Connects to the server
    override fun connect(serverUrl: String, onResult: ConnectResult?) {
        this.serverUrl = serverUrl ?: Package.serverUrl
        val request = Request.Builder().apply {
            url(this@ConnectionImpl.serverUrl).addHeader("Sec-WebSocket-Protocol", "janus-protocol")
        }.build()
        webSocket = OkHttpClient().newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                onResult?.run { onOpen(onResult) }
            }

            override fun onMessage(webSocket: WebSocket, text: String) = onMessage(text)
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) = onFailure(t)
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) = onClosing(code, reason)
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) = onClosed()
        })
    }

    /// Sends message
    override fun sendMessage(message: JSONObject): String? {
        transactionNum += 1
        val transaction = transactionNum.toString(16).padStart(16, '0')
        message.put("transaction", transaction)
        if (sessionId > 0) message.put("session_id", sessionId)
        if (Package.debug) Log.d("SDK/SENDMSG", message.toString())
        webSocket.send(message.toString())
        return transaction
    }

    /// Sends request
    override fun sendRequest(request: JSONObject, onResult: RequestResult): Unit {
        val transaction = sendMessage(request)
        if (transaction != null)
            resultWaiters.put(transaction, Pair(onResult, System.currentTimeMillis() + requestTimeout * 1000))
    }

    /// Closes and destructors connection
    override fun close(error: Throwable?) {
        if (connected) {
            var code = 1000
            var reason = "Connection reset"
            error?.run {
                if (error is JanusError) code = error.code
                if (error is ConnectionError) code = error.code
                message?.let {
                    code = 3000
                    reason = it
                }
            }
            connected = false
            webSocket.close(code, reason)
        } else onClosed()
    }

    /// Adds connection listener
    final override fun addListener(listener: ConnectionListener) = listener.apply { listeners.add(listener) }

    /// Removes connection listener
    final override fun removeListener(listener: ConnectionListener) = listener.apply { listeners.remove(listener) }
}