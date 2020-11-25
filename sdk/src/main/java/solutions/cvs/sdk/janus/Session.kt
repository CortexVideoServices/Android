package solutions.cvs.sdk.janus

import android.util.ArraySet
import android.util.Log
import org.json.JSONObject
import solutions.cvs.sdk.Package
import solutions.cvs.sdk.Stream
import java.io.PrintWriter
import java.io.StringWriter


/// Session listener
interface SessionListener {

    /// Invoked when the connection to the media server has created.
    fun onConnected(session: Session) {}

    /// Invoked when the connection from media server has closed.
    fun onDisconnect(session: Session) {}

    /// Called after local stream has started to publishing
    fun onStreamPublished(session: Session, stream: Stream) {}

    /// Called after local stream has stopped to publishing
    fun onStreamUnpublished(session: Session, stream: Stream) {}

    /// Called after the start of receiving a remote stream from the media server
    fun onStreamCreated(session: Session, stream: Stream) {}

    /// Called after stopping receiving a remote stream from the media server.
    fun onStreamDestroy(session: Session, stream: Stream) {}

    /// Called when session error occurs.
    fun onError(session: Session, reason: Error) {}
}


interface Session {
    /// True if session connected
    val connected: Boolean

    /// Connects to the media server
    fun connect(serverUrl: String, onResult: ConnectResult? = null): Unit
    fun connect(onResult: ConnectResult? = null) = connect(Package.serverUrl, onResult)

    /// Publish stream
    fun publishStream(stream: Stream, participantName: String? = null): Unit

    /// Unpublish stream
    fun unpublishStream(): Unit

    /// Disconnect  from media server
    fun disconnect(): Unit

    /// Adds session listener
    fun addListener(listener: SessionListener): SessionListener

    /// Removes session listener
    fun removeListener(listener: SessionListener): SessionListener
}


/// Base implementation of the  session
abstract class SessionImpl : Session {

    private var publishedStream: Stream? = null
    protected val listeners = ArraySet<SessionListener>()
    private var connectionListener: ConnectionListener


    protected var connection: Connection
        private set

    // === Connection listeners methods ==============================

    /// Invoked when the connection has created.
    protected open fun onConnected() {
        connected = connection.connected
        if (connected)
            listeners.forEach { it.onConnected(this) }
    }

    /// Invoked when the connection has closed.
    protected open fun onDisconnect(reason: Throwable?) {
        reason?.run {
            val error = Error("Connection reset", reason)
            listeners.forEach { it.onError(this@SessionImpl, error) }
        }
        listeners.forEach { it.onDisconnect(this) }
    }

    /// Janus message
    protected open fun onMessage(message: JSONObject) {
        // ToDo: .onStreamPublished .onStreamUnpublished
    }

    /// Invoked when the connection fails.
    protected fun onConnectionError(reason: Throwable) {
        /////connection.close()  // Closed by any problem with connection
    }

    // ============================= Connection listeners methods ====

    init {
        if (Package.debug)
            addListener(object : SessionListener {
                override fun onStreamPublished(session: Session, stream: Stream) {
                    Log.d("SDK/EVENTS", "$session; onStreamPublished: $stream")
                }

                override fun onStreamUnpublished(session: Session, stream: Stream) {
                    Log.d("SDK/EVENTS", "$session; onStreamUnpublished: $stream")
                }

                override fun onStreamCreated(session: Session, stream: Stream) {
                    Log.d("SDK/EVENTS", "$session; onStreamCreated: $stream")
                }

                override fun onStreamDestroy(session: Session, stream: Stream) {
                    Log.d("SDK/EVENTS", "$session; onStreamDestroy: $stream")
                }

                override fun onError(session: Session, reason: Error) {
                    val stackTrace = StringWriter()
                    reason.printStackTrace(PrintWriter(stackTrace))
                    Log.d("SDK/EVENTS", "$session; onError: $reason\n$stackTrace")
                }
            })
        connection = Connection.create()
        connectionListener = connection.addListener(object : ConnectionListener {
            override fun onConnected(connection: Connection) = onConnected()
            override fun onDisconnect(connection: Connection, reason: Throwable?) {
                connected = false;
                onDisconnect(reason)
            }

            override fun onMessage(connection: Connection, message: JSONObject) = onMessage(message)
            override fun onError(connection: Connection, reason: Throwable) = onConnectionError(reason)
        })
    }

    /// True if session connected
    final override var connected: Boolean = false
        private set

    /// Connects to the media server
    override fun connect(serverUrl: String, onResult: ConnectResult?) {
        connection.connect(serverUrl ?: Package.serverUrl) { error ->
            onResult?.let { it(error) }
        }
    }

    override fun publishStream(stream: Stream, participantName: String?) {
        publishedStream = stream
    }

    override fun unpublishStream() {
        publishedStream?.let { stream ->
            listeners.forEach { it.onStreamUnpublished(this, stream) }
            publishedStream = null
        }
    }

    /// Disconnect  from media server
    override fun disconnect() {
        if (connected)
            connection.close()
    }

    /// Adds session listener
    final override fun addListener(listener: SessionListener) = listener.apply { listeners.add(listener) }

    /// Removes session listener
    final override fun removeListener(listener: SessionListener) = listener.apply { listeners.add(listener) }

}