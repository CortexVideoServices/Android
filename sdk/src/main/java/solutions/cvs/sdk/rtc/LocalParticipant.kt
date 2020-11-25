package solutions.cvs.sdk.rtc

import android.view.SurfaceView
import org.webrtc.*
import solutions.cvs.sdk.Publisher
import solutions.cvs.sdk.PublisherObserver
import solutions.cvs.sdk.SDKError
import solutions.cvs.sdk.Session

/// Local participant (publishing stream)
abstract class LocalParticipant(
    private val settings: Publisher.Settings,
    private val session: Session,
    private val observer: PublisherObserver
) : Participant(session.settings, observer.logger), Publisher {

    /// View where rendering stream
    override val view: SurfaceView?
        get() = viewRenderer

    private var viewRenderer: SurfaceViewRenderer? = null

    /// Audio enable/disable switcher
    override var audio: Boolean = settings.audioConstraints != null

    /// Video enable/disable switcher
    override var video: Boolean = settings.run { videoConstraints != null && videoCapturer != null }

    /// Starts capturer
    final override fun startCapturer() {
        if (video) {
            viewRenderer = SurfaceViewRenderer(settings.appContext).apply {
                init(settings.eglBase.eglBaseContext, null)
                setEnableHardwareScaler(true)
                setZOrderMediaOverlay(true)
                setMirror(true)
            }
            streamViewProxy.setTarget(viewRenderer)
        }
        thisThread.post {
            settings.audioConstraints?.let { createAudioTrack(it) }
            settings.videoConstraints?.let { constraints ->
                settings.videoCapturer?.let { videoCapturer ->
                    createVideoTrack(streamViewProxy, videoCapturer, constraints)
                }
            }
            if (audioTrack != null || videoTrack != null)
                observer.onStreamCreated(this)
        }
    }

    /// Starts peer connection
    override fun startPeer() {
        thisThread.post {
            try {
                val peerConnection = this.peerConnection ?: throw SDKError("Cannot create peer connection")
                audioTrack?.let { track -> peerConnection.addTrack(track, listOf(id)) }
                videoTrack?.let { track -> peerConnection.addTrack(track, listOf(id)) }
            } catch (reason: Throwable) {
                observer.onError(SDKError("Cannot start peer", reason))
                stopPeer()
            }
        }
    }

    /// Stop capturer
    override fun stopCapturer() {
        stopPeer()
        if (audioTrack != null || videoTrack != null)
            observer.onStreamDestroy(this)
        thisThread.post {
            streamViewProxy.setTarget(null)
            settings.videoCapturer?.stopCapture()
            surfaceTextureHelper?.dispose(); surfaceTextureHelper = null
            videoSource?.dispose(); videoSource = null
            videoTrack?.dispose(); videoTrack = null
            audioSource?.dispose(); audioSource = null
            audioTrack?.dispose(); audioTrack = null
        }
    }

    override fun destroy() {
        if (audioTrack != null || videoTrack != null)
            stopCapturer()
        viewRenderer?.clearImage()
        viewRenderer?.release()
    }

    private val streamViewProxy = ProxyVideoSink()

    init {
        startCapturer()
    }

    private var audioSource: AudioSource? = null
    protected var audioTrack: AudioTrack? = null

    /// Creates local audio track
    private fun createAudioTrack(constraints: MediaConstraints): AudioTrack? {
        audioSource = factory.createAudioSource(constraints)
        audioTrack = factory.createAudioTrack("${id}_audio_0", audioSource)
        audioTrack?.setEnabled(audio)
        return audioTrack
    }

    private var videoSource: VideoSource? = null
    protected var videoTrack: VideoTrack? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null

    /// Creates local video track
    private fun createVideoTrack(
        localRender: VideoSink,
        videoCapturer: VideoCapturer,
        constraints: VideoConstraints
    ): VideoTrack? {
        val eglBaseContext = settings.eglBase.eglBaseContext
        val context = settings.appContext
        surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBaseContext)
        videoSource = factory.createVideoSource(videoCapturer.isScreencast)
        return videoSource?.let { videoSource ->
            videoCapturer.initialize(surfaceTextureHelper, context, videoSource.capturerObserver)
            videoCapturer.startCapture(constraints.width, constraints.height, constraints.rate)
            videoTrack = factory.createVideoTrack("${id}_video_0", videoSource)
            videoTrack?.apply {
                setEnabled(video)
                addSink(localRender)
            }
        }
    }

}