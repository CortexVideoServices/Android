package solutions.cvs.sdk

import org.json.JSONObject

interface Connection {

    enum class State {
        Initial,
        Connecting,
        Connected,
        Closing,
        Closed
    }

    /// Connection state
    val state: State

    /// Connects to the session server
    fun connect(): Unit

    /// Disconnects from the session
    fun disconnect(): Unit

    /// Connection settings
    interface Settings {
        /// UTL to the session server
        val serverUrl: String

        /// Debug flag
        val debug: Boolean
    }

    /// Connection observer
    interface Observer {
        /// Called when a data error occurred
        fun onError(reason: Throwable): Unit?

        /// Called when the session client connects to the session server.
        fun onConnected(): Unit?

        /// Called when a message received
        fun onMessage(message: JSONObject): Unit?

        /// Called when a connection error occurred
        fun onConnectionError(reason: Throwable): Unit?

        /// Called when the session client has disconnected from the session server.
        fun onDisconnected(): Unit?
    }
}