package solutions.cvs.sdk.janus

import org.json.JSONObject

typealias PluginResult = (error: Throwable?) -> Unit

/// Janus plugin interface
interface Plugin {
    /// True if attached
    val attached: Boolean

    /// Attaches plugin
    fun attach(connection: Connection, onResult: PluginResult)

    /// Detaches plugin
    fun detach()

    /// Sends message
    fun sendMessage(message: JSONObject): String?

    /// Sends request
    fun sendRequest(request: JSONObject, onResult: RequestResult)

    companion object {
        /// Creates plugin
        fun create(pluginName: String): Plugin {
            return PluginImpl(pluginName)
        }
    }
}


/// Janus plugin base implementation
class PluginImpl(val pluginName: String) : Plugin {

    var connection: Connection? = null

    override val attached: Boolean
        get() = connection != null

    /// Attaches plugin
    override fun attach(connection: Connection, onResult: PluginResult) {
        connection.sendRequest(JSONObject(mapOf("janus" to "attach", "plugin" to pluginName))) { result ->
            if (result is JSONObject) {
                val message = result as JSONObject
                handleId = message.getJSONObject("data").getLong("id")
                this@PluginImpl.connection = connection
                onResult(null)
            } else onResult(Error("Cannot attach plugin", if (result is Throwable) result else null))

        }
    }

    /// Detaches plugin
    override fun detach() {
        try {
            sendMessage(JSONObject(mapOf("janus" to "detach")))
        } catch (_: Throwable) {
        } finally {
            this@PluginImpl.connection = null
        }
    }

    private var handleId: Long = 0

    /// Sends message
    override fun sendMessage(message: JSONObject): String? {
        connection?.run {
            if (handleId > 0) message.put("handle_id", handleId)
            return this.sendMessage(message)
        }
        return null
    }

    /// Sends request
    override fun sendRequest(request: JSONObject, onResult: RequestResult) {
        connection?.run {
            if (handleId > 0) request.put("handle_id", handleId)
            return this.sendRequest(request, onResult)
        }
    }

}