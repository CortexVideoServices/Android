package solutions.cvs.sdk.utl

import org.webrtc.*


/// Peer connection observer
open class PeerConnectionObserver(private var logger: ((message: String) -> Unit)? = null) : PeerConnection.Observer {

    override fun onSignalingChange(signalingState: PeerConnection.SignalingState) {
        logger?.invoke("${this}.onSignalingChange: $signalingState")
    }

    override fun onIceConnectionChange(iceConnectionState: PeerConnection.IceConnectionState) {
        logger?.invoke("${this}.onIceConnectionChange: $iceConnectionState")
    }

    override fun onIceConnectionReceivingChange(b: Boolean) {
        logger?.invoke("${this}.onIceConnectionReceivingChange: $b")
    }

    override fun onIceGatheringChange(iceGatheringState: PeerConnection.IceGatheringState) {
        logger?.invoke("${this}.onIceGatheringChange: $iceGatheringState")
    }

    override fun onIceCandidate(iceCandidate: IceCandidate) {
        logger?.invoke("${this}.onIceCandidate: $iceCandidate")
    }

    override fun onIceCandidatesRemoved(iceCandidates: Array<IceCandidate>) {
        logger?.invoke("${this}.onIceCandidatesRemoved: $iceCandidates")
    }

    override fun onAddStream(mediaStream: MediaStream) {
        logger?.invoke("${this}.onAddStreamCalled: $mediaStream")
    }

    override fun onRemoveStream(mediaStream: MediaStream) {
        logger?.invoke("${this}.onRemoveStream: $mediaStream")
    }

    override fun onDataChannel(dataChannel: DataChannel) {
        logger?.invoke("${this}.onDataChannel: $dataChannel")
    }

    override fun onRenegotiationNeeded() {
        logger?.invoke("${this}.onRenegotiationNeeded")
    }

    override fun onAddTrack(rtpReceiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {
        logger?.invoke("${this}.onAddTrack: $rtpReceiver, $mediaStreams")
    }
}
