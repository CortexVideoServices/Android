package solutions.cvs.sdk

import android.content.Context
import org.webrtc.EglBase
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import solutions.cvs.sdk.janus.JanusConnection
import solutions.cvs.sdk.janus.JanusSession
import solutions.cvs.sdk.rtc.Camera


/// Session builder
class SessionBuilder(
    override val appContext: Context,
    override val serverUrl: String,
    override val sessionId: String
) : Session.Settings {

    /// EGL base
    override var eglBase: EglBase = EglBase.create()

    /// Debug flag
    override var debug: Boolean = false

    /// Enables hardware acceleration
    override var hardwareAcceleration: Boolean = true

    /// Default ICE servers list
    private var iceServers = mutableListOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun3.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun4.l.google.com:19302").createIceServer()
    )

    /// RTC Configuration
    override var rtcConfiguration: PeerConnection.RTCConfiguration =
        PeerConnection.RTCConfiguration(iceServers).apply {
            tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED
            bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
            rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            keyType = PeerConnection.KeyType.ECDSA
            enableDtlsSrtp = true
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }

    companion object {
        private var onceInitialized = false
    }

    init {
        if (!onceInitialized) {
            val fieldTrials = "WebRTC-IntelVP8/Enabled/"  // ToDo: Enable H264HighProfile
            PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(appContext)
                    .setFieldTrials(fieldTrials)
                    .setEnableInternalTracer(true)
                    .createInitializationOptions()
            )
            onceInitialized = true
        }
        Camera.initialize(appContext)
    }

    /// Creates session
    fun build(observer: SessionObserver? = null): Session {
        val observer = if (observer is SessionObserver) observer else SessionObserver()
        return JanusSession(this, observer)
    }
}