package solutions.cvs.sdk

import android.content.Context
import android.util.Log
import android.view.View
import org.webrtc.*
import org.webrtc.PeerConnection.IceServer
import org.webrtc.PeerConnection.RTCConfiguration
import solutions.cvs.sdk.webrtc.Camera


/// Returns a random string [0-9,A-Z] with the given size
fun randomString(size: Int): String {
    val alphabet: List<Char> = ('A'..'Z') + ('0'..'9')
    return List(size) { alphabet.random() }.joinToString("")
}


/// Package stream interface
interface Stream {
    /// Stream ID
    val id: String;

    /// Bounded view
    val view: View

    /// Raw media stream
    val stream: MediaStream

    /// Closes and destructs stream
    fun close(): Unit
}


/// Package object
object Package {

    /// RTC configuration
    lateinit var rtcConfiguration: RTCConfiguration
        private set

    /// Application context
    lateinit var applicationContext: Context
        private set

    /// RTC Peer Connection Factory
    lateinit var peerConnectionFactory: PeerConnectionFactory
        private set

    /// URL oh the media server
    lateinit var serverUrl: String
        private set

    /// Debug flag
    var debug: Boolean = true
        private set

    val eglBase = EglBase.create()

    /// Initializes package
    fun init(applicationContext: Context, serverUrl: String, debug: Boolean = false) {
        rtcConfiguration = RTCConfiguration(
            mutableListOf(
                IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
                IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
                IceServer.builder("stun:stun2.l.google.com:19302").createIceServer(),
                IceServer.builder("stun:stun3.l.google.com:19302").createIceServer(),
                IceServer.builder("stun:stun4.l.google.com:19302").createIceServer()
            )
        )
//        rtcConfiguration.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED
//        rtcConfiguration.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE;
//        rtcConfiguration.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE;
//        rtcConfiguration.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY;
//        rtcConfiguration.keyType = PeerConnection.KeyType.ECDSA;
        rtcConfiguration.enableDtlsSrtp = true;
        rtcConfiguration.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN;

        this.applicationContext = applicationContext
        this.serverUrl = serverUrl
        this.debug = debug

        val optionsBuilder = PeerConnectionFactory.InitializationOptions.builder(applicationContext)
        optionsBuilder.setEnableInternalTracer(true)
        val opt = optionsBuilder.createInitializationOptions()
        PeerConnectionFactory.initialize(opt)
        val options = PeerConnectionFactory.Options()

        val encoderFactory: VideoEncoderFactory
        val decoderFactory: VideoDecoderFactory
        encoderFactory = SoftwareVideoEncoderFactory()
        decoderFactory = SoftwareVideoDecoderFactory()

        this.peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .setOptions(options)
            .createPeerConnectionFactory()

        Camera.init(applicationContext)

        if (debug) Log.d("SPELLVIDEO/SDK", "Package successfully has initialized")
    }
}