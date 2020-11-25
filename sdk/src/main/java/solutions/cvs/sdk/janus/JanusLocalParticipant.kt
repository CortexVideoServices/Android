package solutions.cvs.sdk.janus

import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription
import solutions.cvs.sdk.Publisher
import solutions.cvs.sdk.PublisherObserver
import solutions.cvs.sdk.SDKError
import solutions.cvs.sdk.rtc.LocalParticipant

class JanusLocalParticipant(
    private var settings: Publisher.Settings,
    session: JanusSession,
    private var observer: PublisherObserver
) : LocalParticipant(settings, session, observer) {

    override val participantName = settings.participantName

    var plugin: JanusPlugin? = null
    private var roomId: Long = 0

    fun attache(plugin: JanusPlugin, roomId: Long, onResult: (result: Any) -> Unit) {
        this.plugin = plugin
        this.roomId = roomId
        thisThread.post {
            plugin.attach { result ->
                if (result is Throwable)
                    this.plugin = null
                else {
                    joinToRoom { joinResult ->
                        if (joinResult is JSONObject) {
                            startPeer()
                        } else {
                            detach()
                            onResult(joinResult)
                        }
                    }
                }
            }
        }
    }

    fun detach() {
        thisThread.post {
            stopPeer()
            plugin?.detach()
            this.plugin = null
        }
    }

    override fun onRenegotiationNeeded() {
        super.onRenegotiationNeeded()
        val makeError: (Throwable?) -> Exception =
            { reason -> SDKError("Cannot send offer", reason) }
        if (plugin == null)
            observer.onError(makeError(SDKError("Plugin not connected")))
        createOffer(audio, video) { sdrResult ->
            if (sdrResult is SessionDescription)
                sendOffer(sdrResult.description) { result ->
                    if (result is Throwable) observer.onError(result)
                }
            else
                observer.onError(makeError(if (sdrResult is Throwable) sdrResult else null))
        }
    }

    override fun onIceCandidate(iceCandidate: IceCandidate) {
        plugin?.run {
            sendMessage(JSONObject(mapOf("janus" to "trickle", "candidate" to iceCandidate)))
        }
    }

    override fun onIceGatheringChange(iceGatheringState: PeerConnection.IceGatheringState) {
        if (iceGatheringState == PeerConnection.IceGatheringState.COMPLETE)
            plugin?.run {
                sendMessage(
                    JSONObject(
                        mapOf(
                            "janus" to "trickle",
                            "candidate" to JSONObject(mapOf("completed" to true))
                        )
                    )
                )
            }
    }


    var feedId: Long = 0

    private fun createRoom(onResult: (result: Any) -> Unit) {
        val makeError: (Throwable?) -> Exception =
            { reason -> SDKError("Cannot create room", reason) }
        if (plugin == null) {
            onResult(makeError(SDKError("Plugin not connected")))
        } else {
            plugin?.run {
                sendRequest(
                    JSONObject(
                        mapOf(
                            "janus" to "message",
                            "body" to JSONObject(
                                mapOf(
                                    "request" to "create",
                                    "room" to roomId,
                                    "private" to true
                                )
                            )
                        )
                    )
                ) { result ->
                    if (result is JSONObject) onResult(true)
                    else onResult(makeError(if (result is Throwable) result else null))
                }
            }
        }
    }

    private fun joinToRoom(onResult: (result: Any) -> Unit) {
        val makeError: (Throwable?) -> Exception =
            { reason -> SDKError("Cannot join to room", reason) }
        if (plugin == null) {
            onResult(makeError(SDKError("Plugin not connected")))
        } else {
            plugin?.run {
                sendRequest(
                    JSONObject(
                        mapOf(
                            "janus" to "message",
                            "body" to JSONObject(
                                mapOf(
                                    "request" to "join",
                                    "ptype" to "publisher",
                                    "room" to roomId
                                )
                            )
                        )
                    )
                ) { result ->
                    if (result is JSONObject) {
                        val message = result as JSONObject
                        val data = message.getJSONObject("plugindata").getJSONObject("data")
                        this@JanusLocalParticipant.feedId = data.getLong("id")
                        onResult(data)
                    } else {
                        if (result is JanusError) {
                            if (result.code == 426) {
                                createRoom { createResult ->
                                    if (createResult is Exception)
                                        onResult(makeError(result))
                                    else
                                        joinToRoom(onResult)
                                }
                            }
                            onResult(makeError(if (result is Throwable) result else null))
                        }

                    }
                }
            }
        }
    }

    private fun sendOffer(localSdr: String, onResult: (result: Any) -> Unit) {
        val makeError: (Throwable?) -> Exception =
            { reason -> SDKError("Cannot send offer", reason) }
        if (plugin == null)
            onResult(makeError(SDKError("Plugin not connected")))
        plugin?.run {
            sendRequest(
                JSONObject(
                    mapOf(
                        "janus" to "message",
                        "body" to JSONObject(
                            mapOf(
                                "request" to "configure",
                                "audio" to audio,
                                "video" to video,
                                "display" to (participantName ?: "")
                            )
                        ),
                        "jsep" to JSONObject(
                            mapOf(
                                "type" to "offer",
                                "sdp" to localSdr
                            )
                        )
                    )
                )
            ) { answerResult ->
                if (answerResult is JSONObject) {
                    val sdp = answerResult.getJSONObject("jsep").getString("sdp")
                    val remoteSdp = SessionDescription(SessionDescription.Type.ANSWER, sdp)
                    applyAnswer(remoteSdp, onResult)
                } else
                    onResult(makeError(if (answerResult is Throwable) answerResult else null))
            }
        }
    }
}


