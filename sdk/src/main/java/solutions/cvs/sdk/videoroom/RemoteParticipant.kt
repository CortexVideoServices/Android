package solutions.cvs.sdk.videoroom

import org.json.JSONObject
import org.webrtc.MediaStream
import org.webrtc.SessionDescription
import solutions.cvs.sdk.janus.Participant
import solutions.cvs.sdk.janus.PeerResult

typealias StreamAwaiter = (stream: MediaStream) -> Unit

class RemoteParticipant() : Participant("janus.plugin.videoroom") {

    var mediaStream: MediaStream? = null
        private set

    private var streamAwaiter: StreamAwaiter? = null
    private fun awaitStream(onResult: PeerResult) {
        if (mediaStream != null) onResult(mediaStream)
        else streamAwaiter = { stream -> onResult(stream) }
    }

    override fun onAddStream(p0: MediaStream?) {
        super.onAddStream(p0)
        p0?.let { stream ->
            mediaStream = stream
            streamAwaiter?.let { callback ->
                callback(stream)
            }
        }
    }

    fun start(roomId: Long, feedId: Long, privateId: Long, onResult: PeerResult) {
        joinToRoom(roomId, feedId, privateId) { result ->
            if (result is SessionDescription) {
                val remoteOffer = result as SessionDescription
                applyOffer(remoteOffer) { result ->
                    if (result is Boolean && result) {
                        createAnswer { result ->
                            if (result is SessionDescription) {
                                val answer = result as SessionDescription
                                startSubscribing(roomId, answer, onResult)
                                return@createAnswer
                            }
                            onResult(if (result is Throwable) result else IllegalStateException("Unexpected response"))
                        }
                        return@applyOffer
                    }
                    onResult(if (result is Throwable) result else IllegalStateException("Unexpected response"))
                }
                return@joinToRoom
            }
            onResult(if (result is Throwable) result else IllegalStateException("Unexpected response"))
        }
    }

    /// Joins subscriber to room
    protected fun joinToRoom(roomId: Long, feedId: Long, privateId: Long, onResult: PeerResult): Unit {
        sendRequest(
            JSONObject(
                mapOf(
                    "janus" to "message",
                    "body" to JSONObject(
                        mapOf(
                            "request" to "join",
                            "ptype" to "subscriber",
                            "room" to roomId,
                            "feed" to feedId,
                            "private_id" to privateId
                        )
                    )
                )
            )
        ) { result ->
            if (result is JSONObject) {
                val description = result.getJSONObject("jsep").getString("sdp")
                val remoteOffer = SessionDescription(SessionDescription.Type.OFFER, description)
                onResult(remoteOffer)
            } else onResult(if (result is Throwable) result else IllegalStateException("Unexpected response"))
        }
    }

    /// Starts subscribing
    protected fun startSubscribing(roomId: Long, answer: SessionDescription, onResult: PeerResult): Unit {
        sendRequest(
            JSONObject(
                mapOf(
                    "janus" to "message",
                    "body" to JSONObject(
                        mapOf(
                            "request" to "start",
                            "room" to roomId
                        )
                    ),
                    "jsep" to JSONObject(
                        mapOf(
                            "type" to "answer",
                            "sdp" to answer.description
                        )
                    )
                )
            )
        ) { result ->
            if (result is JSONObject) {
                awaitStream(onResult)
            } else onResult(if (result is Throwable) result else IllegalStateException("Unexpected response"))
        }
    }

}