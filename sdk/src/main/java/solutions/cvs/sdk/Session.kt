package solutions.cvs.sdk

import android.content.Context
import org.json.JSONObject
import org.webrtc.EglBase
import org.webrtc.PeerConnection


interface Session {

    /// Settings
    val settings: Settings

    /// Connection
    val connection: Connection

    /// Connects to the session server
    fun connect(): Unit

    /// Starts a publisher streaming to the session.
    fun publish(publisher: Publisher): Unit

    /// Disconnects the publisher from the session.
    fun unpublish(publisher: Publisher): Unit

    /// Disconnects from the session
    fun disconnect(): Unit

    /// Session settings
    interface Settings : Connection.Settings {

        /// Application context
        val appContext: Context

        /// Session ID
        val sessionId: String

        /// EGL base
        var eglBase: EglBase

        /// RTC configuration
        val rtcConfiguration: PeerConnection.RTCConfiguration

        /// Hardware acceleration switcher
        val hardwareAcceleration: Boolean
    }

    /// Session observer
    interface Observer  {

        /// Called when an error occurred
        fun onError(reason: Throwable): Unit?

        /// Called when the session client connects to the session server.
        fun onConnected(): Unit?

        /// Called when a connection error occurred
        fun onConnectionError(reason: Throwable): Unit?

        /// Called when the session client has disconnected from the session server.
        fun onDisconnected(): Unit?

        /// Called when a new stream is received published for this session.
        fun onStreamReceived(stream: Stream): Unit?

        /// Called when stops publishing a stream to this session.
        fun onStreamDropped(stream: Stream): Unit?
    }
}