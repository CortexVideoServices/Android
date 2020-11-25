package solutions.cvs.sdk

import solutions.cvs.sdk.janus.Connection
import solutions.cvs.sdk.janus.ConnectionListener
import solutions.cvs.sdk.videoroom.VideoroomSession


/// Session interface
interface Session : solutions.cvs.sdk.janus.Session {
    /// Session ID
    val id: String

    companion object {
        /// Create session
        fun create(sessionId: String): Session {
            return SessionImpl(sessionId)
        }
    }
}

internal class SessionImpl(
    override val id: String
) : VideoroomSession(id.toLong(36)), Session {

    init {
        var reconnect = false
        connection.addListener(object : ConnectionListener {
            override fun onError(connection: Connection, reason: Throwable) {
                reconnect = true
                connection.close(reason)
            }

            override fun onDisconnect(connection: Connection, reason: Throwable?) {
                if (reason != null && reconnect) {
                    reconnect = false
                    connect()
                }
            }
        })
    }
}