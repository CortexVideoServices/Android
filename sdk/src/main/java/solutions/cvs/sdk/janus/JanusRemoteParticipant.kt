package solutions.cvs.sdk.janus

import org.json.JSONObject
import org.webrtc.SessionDescription
import solutions.cvs.sdk.SDKError
import solutions.cvs.sdk.Session
import solutions.cvs.sdk.rtc.RemoteParticipant


/// Remote Janus participant
class JanusRemoteParticipant(
    settings: Session.Settings,
    private var logger: ((message: String) -> Unit)? = null
) : RemoteParticipant(settings, logger) {

    var plugin: JanusPlugin? = null
    private var roomId: Long = 0
    private var feedId: Long = 0

    override var participantName = null

    fun attache(plugin: JanusPlugin, roomId: Long, feedId: Long, privateId: Long, onResult: (result: Any) -> Unit) {
        this.plugin = plugin
        this.roomId = roomId
        this.feedId = feedId
        thisThread.post {
            plugin.attach { result ->
                if (result is Throwable)
                    this.plugin = null
                else {
                    joinToRoom(privateId) { joinResult ->
                        if( joinResult is SessionDescription) {
                            logger?.invoke("HAS Offer: $joinResult")
                            sendAnswer { sendResult->
                                onResult(sendResult)
                            }
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

    private fun joinToRoom(privateId: Long, onResult: (result: Any) -> Unit) {
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
                        applyOffer(remoteOffer) { applyResult ->
                            if (applyResult is Throwable) onResult(makeError(applyResult))
                            else onResult(remoteOffer)
                        }
                    } else
                        onResult(makeError(if (result is Throwable) result else null))
                }
            }
        }
    }

    private fun sendAnswer(onResult: (result: Any) -> Unit) {
        val makeError: (Throwable?) -> Exception =
            { reason -> SDKError("Cannot send answer", reason) }
        if (plugin == null) {
            onResult(makeError(SDKError("Plugin not connected")))
        } else {
            plugin?.run {
                createAnswer { answerResult->
                    if (answerResult is SessionDescription) {
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
                                            "sdp" to answerResult.description
                                        )
                                    )
                                )
                            )
                        ) { result->
                            if(result is JSONObject)
                                onResult(result)
                            else
                                onResult(makeError(if (answerResult is Throwable) answerResult else null))
                        }
                    } else
                        onResult(makeError(if (answerResult is Throwable) answerResult else null))
                }
            }
        }
    }
}

