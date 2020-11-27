package solutions.cvs.sdk.janus

import org.json.JSONObject
import solutions.cvs.sdk.*
import solutions.cvs.sdk.Session.*
import solutions.cvs.sdk.rtc.RemoteParticipant

/// Janus session
class JanusSession(
    override val settings: Settings,
    private val observer: Observer
) : Session {

    private val conferenceSessionId = settings.sessionId
    private var roomDescription: String = ""
    private var privateId: Long = 0


    private lateinit var janusConnection: JanusConnection
    override val connection: Connection
        get() = janusConnection

    init {
        resetConnection()
    }

    private fun resetConnection() {
        janusConnection = JanusConnection(
            settings, ConnectionObserver(
                onConnected = { onConnected() },
                onDisconnected = { onDisconnected() },
                onMessage = { message -> onMessage(message) },
                onConnectionError = { reason -> onConnectionError(reason) },
                onError = { reason -> onError(reason) },
                logger = if (observer is SessionObserver) observer.logger else null
            )
        )
    }

    /// Connects to the session server
    override fun connect() {
        janusConnection.connect()
    }

    /// Disconnects from the session
    override fun disconnect() {
        connection.disconnect()
    }

    /// Starts a publisher streaming to the session.
    override fun publish(publisher: Publisher) {
        attachePublisher(publisher as JanusLocalParticipant)
    }

    /// Disconnects the publisher from the session.
    override fun unpublish(publisher: Publisher) {
        detachePublisher(publisher as JanusLocalParticipant)
    }

    private val publishers = mutableMapOf<String, JanusLocalParticipant>()
    private val subscribers = mutableMapOf<Long, JanusRemoteParticipant>()

    private fun addSubscriber(feedId: Long) {
        val makeError: (Throwable?) -> Exception =
            { reason -> SDKError("Cannot subscribe stream", reason) }
        if (!subscribers.containsKey(feedId)) {
            val subscriber = JanusRemoteParticipant(settings)
            val plugin = JanusPlugin(janusConnection)
            subscriber.attache(plugin, conferenceSessionId, feedId, privateId) { attachResult ->
                if (attachResult is JSONObject) {
                    subscribers.put(feedId, subscriber)
                    observer.onStreamReceived(subscriber)
                } else
                    observer.onError(makeError(if (attachResult is Throwable) attachResult else null))
            }
        }
    }

    private fun removeSubscriber(feedId: Long) {
        val remoteParticipant = subscribers.remove(feedId)
        remoteParticipant?.run {
            observer.onStreamDropped(remoteParticipant)
            try {
                remoteParticipant.detach()
                remoteParticipant.destroy()
            } catch (reason: Exception) {}
        }
    }

    private fun onConnected() {
        publishers.values.forEach({ publisher -> attachePublisher(publisher) })
        observer.onConnected()
    }

    private fun onDisconnected() {
        observer.onDisconnected()
        subscribers.keys.toList().forEach { feedId-> removeSubscriber(feedId) }
        publishers.values.forEach({ publisher -> detachePublisher(publisher) })
        resetConnection()
    }

    private fun attachePublisher(publisher: JanusLocalParticipant) {
        val makeError: (Throwable?) -> Exception =
            { reason -> SDKError("Cannot publish stream", reason) }
        try {
            if (connection.state == Connection.State.Connected) {
                val plugin = JanusPlugin(janusConnection)
                publisher.attache(plugin, conferenceSessionId) { attachResult ->
                    if (attachResult is JSONObject) {
                        roomDescription = attachResult.optString("description")
                        privateId = attachResult.optLong("private_id", 0)
                        publishers.set(publisher.id, publisher)
                    } else
                        observer.onError(makeError(if (attachResult is Throwable) attachResult else null))
                }
            }
        } catch (reason: Exception) {
            observer.onError(makeError(reason))
        }
    }

    private fun detachePublisher(publisher: JanusLocalParticipant) {
        if (!(publisher.id in publishers)) return
        try {
            if (connection.state == Connection.State.Connected) {
                publisher.detach()
                publisher.destroy()
            }
        } catch (reason: Exception) {
        } finally {
            publishers.remove(publisher.id)
        }
    }

    private fun onMessage(message: JSONObject) {
        if (message.has("plugindata")) {
            val data = message.getJSONObject("plugindata").getJSONObject("data")
            data.optJSONArray("publishers")?.let {
                for (i in 0..(it.length() - 1)) {
                    if (it[i] is JSONObject) {
                        val item = it[i] as JSONObject
                        val feedId = item.getLong("id")
                        addSubscriber(feedId)
                    }
                }
            }
            data.optLong("unpublished", 0).let { id ->
                if (id > 0) removeSubscriber(id)
            }
        }
    }

    private fun onConnectionError(reason: Throwable) {
        // TODO("Not yet implemented")
    }

    private fun onError(reason: Throwable) {
        // TODO("Not yet implemented")
    }

}