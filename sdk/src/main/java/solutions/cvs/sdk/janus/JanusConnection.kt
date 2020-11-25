package solutions.cvs.sdk.janus

import android.util.Log
import okhttp3.*
import org.json.JSONObject
import solutions.cvs.sdk.Connection
import solutions.cvs.sdk.Connection.*
import solutions.cvs.sdk.Connection.Observer
import solutions.cvs.sdk.ConnectionObserver
import solutions.cvs.sdk.SDKError
import java.lang.Exception
import java.util.*
import java.util.concurrent.Executors
import kotlin.concurrent.schedule


/// Janus connection
class JanusConnection(
    private val settings: Settings,
    private val observer: ConnectionObserver,
    private val requestTimeout: Long = 30,
    private val keepaliveTimeout: Long = 50
) : WebSocketListener(), Connection {

    /// Connection state
    override var state = State.Initial
        private set

    /// Connects to the session server
    override fun connect() {
        state = State.Connecting
        val request = Request.Builder().apply {
            url(settings.serverUrl).addHeader("Sec-WebSocket-Protocol", "janus-protocol")
        }.build()
        webSocket = OkHttpClient().newWebSocket(request, this)
    }

    /// Disconnects from the session
    override fun disconnect() {
        state = State.Closing
        webSocket.close(1000, "Connection reset")
    }

    /// Sends message
    fun sendMessage(message: JSONObject) {
        send(message)
    }

    /// Sends request
    fun sendRequest(request: JSONObject, onResult: (Any) -> Unit) {
        val transaction = send(request)
        resultWaiters.put(
            transaction,
            Pair(onResult, System.currentTimeMillis() + requestTimeout * 1000)
        )
    }

    private lateinit var webSocket: WebSocket
    private val resultWaiters = mutableMapOf<String, Pair<(result: Any) -> Unit, Long>>()

    private var transactionNum = 0
    private var sessionId: Long = 0

    override fun onOpen(webSocket: WebSocket, response: Response) {
        try {
            sendRequest(JSONObject(mapOf("janus" to "create"))) { message ->
                if (message is JSONObject) {
                    sessionId = message.getJSONObject("data").getLong("id")
                    keepalive()
                    state = State.Connected
                    observer.onConnected()
                } else
                    throw (message as Exception)
            }
        } catch (reason: Exception) {

            webSocket.close(1000, null)
            observer.onConnectionError(SDKError("Cannot create connection", reason))
        }
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        val message = JSONObject(text)
        val transaction = message.optString("transaction", "")
        val value = resultWaiters.get(transaction)
        value?.apply {
            val (onComplete, _) = value
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
                    onComplete(JanusError(code, reason))
                }
            } else {
                if (message.optJSONObject("data") != null || plugindata != null) {
                    resultWaiters.remove(transaction)
                    onComplete(message)
                }
            }
        }
        observer.onMessage(message)
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        observer.onConnectionError(SDKError("WebSocket connection error", t))
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        if (state != State.Connecting)
            state = State.Closing
        val error = {
            if (code != 1000) SDKError("Connection reset by error; $code $reason")
            else SDKError("Connection reset")
        }
        resultWaiters.values.forEach { it.first(error) }
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        resultWaiters.clear()
        if (state != State.Connecting) {
            state = State.Closed
            if (code != 1000)
                observer.onConnectionError(SDKError("Connection reset by error: $code:$reason"))
            observer.onDisconnected()
        }
    }

    private fun send(message: JSONObject): String {
        transactionNum += 1
        val transaction = transactionNum.toString(16).padStart(16, '0')
        message.put("transaction", transaction)
        if (sessionId > 0) message.put("session_id", sessionId)
        if (settings.debug && observer.logger!=null)
            observer.logger?.invoke("Sends message to server: $message")
        webSocket.send(message.toString())
        return transaction
    }

    private fun keepalive(timeout: Long = 20) {
        // ToDo: Check requests timeout
        send(JSONObject(mapOf("janus" to "keepalive")))
        Timer("JanusKeepAlive", false).schedule(keepaliveTimeout * 1000) {
            keepalive(timeout)
        }
    }
}