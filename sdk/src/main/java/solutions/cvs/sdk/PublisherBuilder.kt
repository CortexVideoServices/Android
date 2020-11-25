package solutions.cvs.sdk

import org.webrtc.VideoCapturer
import solutions.cvs.sdk.janus.JanusLocalParticipant
import solutions.cvs.sdk.janus.JanusSession
import solutions.cvs.sdk.rtc.AudioConstraints
import solutions.cvs.sdk.rtc.Camera
import solutions.cvs.sdk.rtc.VideoConstraints

/// Publisher builder
class PublisherBuilder(
    var session: Session
) : Publisher.Settings {

    override val appContext = session.settings.appContext
    override val serverUrl = session.settings.serverUrl
    override val sessionId = session.settings.sessionId
    override var eglBase = session.settings.eglBase
    override var debug = session.settings.debug
    override var hardwareAcceleration = session.settings.hardwareAcceleration
    override var rtcConfiguration = session.settings.rtcConfiguration
    override val audioConstraints = AudioConstraints()
    override val videoConstraints = VideoConstraints()
    override var videoCapturer: VideoCapturer? = null
    override var participantName = null

    /// Sets camera capturer
    fun setCameraCapturer(deviceId: String? = null) {
        videoCapturer?.dispose(); videoCapturer = null
        videoCapturer = Camera.createCapturer(deviceId)
    }

    /// Disables video call
    fun disableVideoCall() {
        videoCapturer = null
    }

    companion object {
        private var onceInitialized = false
    }

    init {
        if (!onceInitialized) {
            Camera.initialize(appContext)
            onceInitialized = true
        }
        Camera.default?.let { setCameraCapturer(it.deviceId) }
    }

    /// Creates publisher
    fun build(observer: PublisherObserver? = null) : Publisher {
        val observer = if (observer is PublisherObserver) observer else PublisherObserver()
        return JanusLocalParticipant(this, session as JanusSession, observer)
    }
}