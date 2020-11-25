package solutions.cvs.sdk.janus

import android.util.Log
import org.json.JSONObject
import org.webrtc.*
import solutions.cvs.sdk.Package
import java.util.*


typealias PeerResult = (result: Any?) -> Unit

class SdpException(
    message: String?,
    cause: Throwable? = null,
    enableSuppression: Boolean = true,
    writableStackTrace: Boolean = true
) :
    Exception(message, cause, enableSuppression, writableStackTrace) {}


open class Participant(pluginName: String) : PeerConnection.Observer, Plugin {

    private val plugin = Plugin.create(pluginName)

    // >>> Implementation of solutions.cvs.sdk.janus.Plugin =================
    override val attached: Boolean
        get() = plugin.attached

    override fun attach(connection: Connection, onResult: PluginResult) = plugin.attach(connection, onResult)
    override fun detach() = plugin.detach()
    override fun sendMessage(message: JSONObject) = plugin.sendMessage(message)
    override fun sendRequest(request: JSONObject, onResult: RequestResult) = plugin.sendRequest(request, onResult)
    // ================= Implementation of solutions.cvs.sdk.janus.Plugin <<<

    override fun onDataChannel(p0: DataChannel?) {
        if (Package.debug) Log.d("D/SDK/PEER", "$this; onDataChannel: $p0")
    }

    override fun onIceCandidate(p0: IceCandidate?) {
        if (Package.debug) Log.d("D/SDK/PEER", "$this; onIceCandidate: $p0")
    }

    override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {
        if (Package.debug) Log.d("D/SDK/PEER", "$this; onIceCandidatesRemoved: $p0")
    }

    override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {
        if (Package.debug) Log.d("D/SDK/PEER", "$this; onIceConnectionChange: $p0")
    }

    override fun onIceConnectionReceivingChange(p0: Boolean) {
        if (Package.debug) Log.d("D/SDK/PEER", "$this; onIceConnectionReceivingChange: $p0")
    }

    override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
        if (Package.debug) Log.d("D/SDK/PEER", "$this; onIceGatheringChange: $p0")
    }

    override fun onSignalingChange(p0: PeerConnection.SignalingState?) {
        if (Package.debug) Log.d("D/SDK/PEER", "$this; onSignalingChange: $p0")
    }

    override fun onRenegotiationNeeded() {
        if (Package.debug) Log.d("D/SDK/PEER", "$this; onRenegotiationNeeded")
    }

    override fun onAddStream(p0: MediaStream?) {
        if (Package.debug) Log.d("D/SDK/PEER", "$this; onAddStream: $p0")
    }

    override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {
        if (Package.debug) Log.d("D/SDK/PEER", "$this; onAddTrack: $p0")
    }

    override fun onRemoveStream(p0: MediaStream?) {
        if (Package.debug) Log.d("D/SDK/PEER", "$this; onRemoveStream: $p0")
    }

    protected val peerConnection = Package.peerConnectionFactory.createPeerConnection(Package.rtcConfiguration, this)
    private val audioRtpSenders = mutableMapOf<String, RtpSender>()
    private val videoRtpSenders = mutableMapOf<String, RtpSender>()

    val constraints = MediaConstraints()

    init {
        constraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        constraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
    }

    fun close() {
        peerConnection?.close()
    }

    fun createOffer(mediaStream: MediaStream, peerResult: PeerResult) {
        val observer = { onResult: (sdr: SessionDescription?) -> Unit ->
            object : SdpObserver {
                override fun onSetSuccess() = onResult(null)
                override fun onSetFailure(p0: String?) = peerResult(Error(p0))
                override fun onCreateFailure(p0: String?) = peerResult(Error(p0))
                override fun onCreateSuccess(p0: SessionDescription?) = onResult(p0)
            }
        }
        peerConnection?.run {
            val streamIds = ArrayList<String>()
            for (audioTrack in mediaStream.audioTracks) {
                val audioSender = peerConnection.addTrack(audioTrack, streamIds)
                audioRtpSenders.put(mediaStream.id, audioSender)
            }
            for (videoTrack in mediaStream.videoTracks) {
                val videoSender = peerConnection.addTrack(videoTrack, streamIds)
                videoRtpSenders.put(mediaStream.id, videoSender)
            }

            this.createOffer(observer { sdr ->
                peerConnection.setLocalDescription(observer { result ->
                    peerResult(sdr)
                }, sdr)
            }, constraints)
        }
    }

    fun applyAnswer(sdr: SessionDescription, peerResult: PeerResult) {
        val observer = { onResult: (sdr: SessionDescription?) -> Unit ->
            object : SdpObserver {
                override fun onSetSuccess() = onResult(null)
                override fun onSetFailure(p0: String?) = peerResult(Error(p0))
                override fun onCreateFailure(p0: String?) = peerResult(Error(p0))
                override fun onCreateSuccess(p0: SessionDescription?) = onResult(p0)
            }
        }
        peerConnection?.run {
            this.setRemoteDescription(observer { result ->
                peerResult(result)
            }, sdr)
        }
    }


    /// Makes SdpObserver
    private fun makeSdpObserver(onResult: PeerResult): SdpObserver {
        return object : SdpObserver {
            override fun onSetSuccess() = onResult(true)
            override fun onSetFailure(p0: String?) = onResult(SdpException(p0))
            override fun onCreateFailure(p0: String?) = onResult(SdpException(p0))
            override fun onCreateSuccess(p0: SessionDescription?) = onResult(p0)
        }
    }

    /// Applies a remote offer
    protected fun applyOffer(remoteOffer: SessionDescription, onResult: PeerResult): Unit {
        peerConnection?.run {
            return setRemoteDescription(makeSdpObserver(onResult), remoteOffer)
        }
        onResult(IllegalStateException("Peer connection is not initialized"))
    }

    /// Creates answer for a remote offer
    protected fun createAnswer(onResult: PeerResult): Unit {
        peerConnection?.run {
            return createAnswer(makeSdpObserver { result ->
                if (result is SessionDescription) {
                    var sdr = result as SessionDescription
                    peerConnection.setLocalDescription(makeSdpObserver { result ->
                        if (result is Boolean && result) {
                            onResult(sdr)
                            return@makeSdpObserver
                        }
                        onResult(if (result is Throwable) result else IllegalStateException("Unexpected response"))
                    }, sdr)
                    return@makeSdpObserver
                }
                onResult(if (result is Throwable) result else IllegalStateException("Unexpected response"))
            }, constraints)
        }
        onResult(IllegalStateException("Peer connection is not initialized"))
    }
}