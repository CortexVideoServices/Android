package solutions.cvs.sdk.janus

import org.json.JSONObject
import solutions.cvs.sdk.Connection

class JanusPlugin(
    private var connection: JanusConnection?
) {

    private var handleId: Long = 0

    /// Attach participant to signal channel
    fun attach(onResult: (result: Any) -> Unit) {
        handleId = 0
        sendRequest(
            JSONObject(
                mapOf(
                    "janus" to "attach",
                    "plugin" to "janus.plugin.videoroom"
                )
            )
        ) { message ->
            if (message is JSONObject) {
                handleId = message.getJSONObject("data").getLong("id")
                onResult(handleId)
            } else onResult(message)
        }
    }

    /// Detach participant from signal channel
    fun detach() {
        try {
            sendMessage(JSONObject(mapOf("janus" to "detach")))
        } catch (reason: Throwable) {
        } finally {
            this.connection = null
        }
    }

    /// Sends message
    fun sendMessage(message: JSONObject): Unit {
        connection?.let { connection ->
            if (handleId > 0) message.put("handle_id", handleId)
            connection.sendMessage(message)
        }
    }

    /// Sends request
    fun sendRequest(request: JSONObject, onResult: (Any) -> Unit): Unit {
        connection?.let { connection ->
            if (handleId > 0) request.put("handle_id", handleId)
            connection.sendRequest(request, onResult)
        }
    }
}