package solutions.cvs.sdk.videoroom

import android.os.Handler
import org.json.JSONObject
import org.webrtc.MediaStream
import org.webrtc.SessionDescription
import solutions.cvs.sdk.Package
import solutions.cvs.sdk.Stream
import solutions.cvs.sdk.janus.JanusError
import solutions.cvs.sdk.janus.PluginResult
import solutions.cvs.sdk.janus.SessionImpl
import solutions.cvs.sdk.webrtc.RemoteStream


data class Publisher(
    val id: Long,
    val display: String?,
    val audioCodec: String?,
    val videoCodec: String?,
    var talking: Boolean,
    var remoteParticipant: RemoteParticipant? = null,
    var remoteStream: Stream? = null
) {
    companion object {
        fun create(item: JSONObject): Publisher {
            val id = item.getLong("id")
            val display = item.optString("display", "")
            val audioCodec = item.optString("audio_codec", "")
            val videoCodec = item.optString("video_codec", "")
            val talking = item.optBoolean("talking", false)
            return Publisher(
                id,
                if (display != "") display else null,
                if (audioCodec == "") audioCodec else null,
                if (videoCodec == "") videoCodec else null,
                talking
            )
        }
    }
}


/// Implementation of the videoroom session
open class VideoroomSession(
    val roomId: Long
) : SessionImpl() {

    private val localParticipant = LocalParticipant()
    private var roomDescription: String? = null
    private var feedId: Long = 0
    private var privateId: Long = 0
    private var publishers = mutableMapOf<Long, Publisher>()


    private fun createRoom(onResult: PluginResult) {
        localParticipant.sendRequest(
            JSONObject(
                mapOf(
                    "janus" to "message",
                    "body" to JSONObject(mapOf("request" to "create", "room" to roomId, "private" to true))
                )
            )
        ) { result ->
            if (result is JSONObject) onResult(null)
            else onResult(Error("Cannot create room", if (result is Throwable) result else null))
        }
    }

    private fun joinToRoom(onResult: PluginResult) {
        localParticipant.sendRequest(
            JSONObject(
                mapOf(
                    "janus" to "message",
                    "body" to JSONObject(mapOf("request" to "join", "ptype" to "publisher", "room" to roomId))
                )
            )
        ) { result ->
            if (result is JSONObject) {
                val message = result as JSONObject
                val data = message.getJSONObject("plugindata").getJSONObject("data")
                roomDescription = data.optString("description")
                feedId = data.getLong("id")
                privateId = data.optLong("private_id", 0)
                onResult(null)
            } else {
                if (result is JanusError) {
                    if (result.code == 426) {
                        createRoom { fail ->
                            if (fail != null) onResult(fail)
                            joinToRoom(onResult)
                        }
                        return@sendRequest
                    }
                }
                onResult(Error("Cannot join to room", if (result is Throwable) result else null))
            }
        }
    }


    private fun addPublisher(publisher: Publisher) {
        publisher.remoteParticipant = RemoteParticipant()
        publisher.remoteParticipant?.let { remoteParticipant ->
            remoteParticipant.attach(connection) { result ->
                if (result != null) {
                    listeners.onEach { it.onError(this, Error("Cannot initialize remote participant", result)) }
                    return@attach
                }
                remoteParticipant.start(roomId, publisher.id, privateId) { resultA ->
                    if (resultA is Throwable) {
                        listeners.onEach { it.onError(this, Error("Cannot start subscribing", resultA)) }
                        return@start
                    } else
                        if (resultA is MediaStream) {
                            Handler(Package.applicationContext.mainLooper).post {
                                val stream = RemoteStream(resultA)
                                publisher.remoteStream = stream
                                publishers.put(publisher.id, publisher)
                                listeners.forEach { it.onStreamCreated(this, stream) }
                            }
                        }
                }
            }
        }
    }

    private fun removePublisher(id: Long) {
        if (publishers.containsKey(id)) {
            val publisher = publishers[id]
            publisher?.remoteStream?.let { stream ->
                listeners.forEach {
                    Handler(Package.applicationContext.mainLooper).post {
                        it.onStreamDestroy(this, stream)
                    }
                }
            }
            publisher?.let { publisher ->
                publisher.remoteParticipant?.let { participant ->
                    participant.detach()
                    participant.close()
                }
            }
            publishers.remove(id)
        }
    }

    /// Invoked when the connection has created.
    override fun onConnected() {
        localParticipant.attach(connection) { fail ->
            fail?.let {
                val error = Error("Cannot initialize local participant; ${it.message}", it)
                listeners.forEach { it.onError(this, error) }
                connection.close(error)
                return@attach
            }
            joinToRoom { failA ->
                failA?.let {
                    val error = Error("Cannot initialize local participant; ${it.message}", it)
                    listeners.forEach { it.onError(this, error) }
                    connection.close(error)
                    return@joinToRoom
                }
                super.onConnected()
            }
        }
    }

    /// Janus message
    override fun onMessage(message: JSONObject) {
        if (message.has("plugindata")) {
            val data = message.getJSONObject("plugindata").getJSONObject("data")
            data.optJSONArray("publishers")?.let {
                for (i in 0..(it.length() - 1)) {
                    if (it[i] is JSONObject) {
                        val item = it[i] as JSONObject
                        val id = item.getLong("id")
                        if (!publishers.containsKey(id))
                            addPublisher(Publisher.create(item))
                    }
                }
            }
            data.optLong("unpublished", 0).let { id ->
                if (id > 0) removePublisher(id)
            }
        }
    }

    /// Publish stream
    override fun publishStream(stream: Stream, participantName: String?) {
        localParticipant.createOffer(stream.stream) { result ->
            if (result is SessionDescription) {
                val locaSdp = result as SessionDescription
                localParticipant.sendRequest(
                    JSONObject(
                        mapOf(
                            "janus" to "message",
                            "body" to JSONObject(
                                mapOf(
                                    "request" to "configure",
                                    "audio" to true,
                                    "video" to true,
                                    "display" to (participantName ?: "")
                                )
                            ),
                            "jsep" to JSONObject(
                                mapOf(
                                    "type" to "offer",
                                    "sdp" to locaSdp.description
                                )
                            )
                        )
                    )
                ) { resultA ->
                    if (resultA is JSONObject) {
                        val message = resultA as JSONObject
                        val data = message.getJSONObject("plugindata").getJSONObject("data")
                        val sdp = message.getJSONObject("jsep").getString("sdp")
                        val remoteSdp = SessionDescription(SessionDescription.Type.ANSWER, sdp)
                        localParticipant.applyAnswer(remoteSdp) { result ->
                            if (result is Throwable) {
                                val error = Error("Cannot apply remote answer", result)
                                listeners.forEach { it.onError(this, error) }
                            }
                        }

                    } else {
                        val error = Error("Cannot configure publisher", if (result is Throwable) result else null)
                        listeners.forEach { it.onError(this, error) }
                    }
                }
            } else {
                val error = Error("Cannot create local offer", if (result is Throwable) result else null)
                listeners.forEach { it.onError(this, error) }
            }
        }
    }

    /// Unpublish stream
    override fun unpublishStream() {
        TODO("Not yet implemented")
    }

    /// Invoked when the connection has closed.
    override fun onDisconnect(reason: Throwable?) {
        publishers.values.forEach { publisher ->
            publisher.remoteStream?.let { remoteStream ->
                listeners.forEach { it.onStreamDestroy(this, remoteStream) }
                remoteStream.close()
            }
        }
        publishers.clear()
        localParticipant.detach()
        super.onDisconnect(reason)
    }


}
