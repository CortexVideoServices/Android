package solutions.cvs.sdk.webrtc

import android.view.SurfaceView
import org.webrtc.MediaStream
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoCapturer
import solutions.cvs.sdk.Package
import solutions.cvs.sdk.Stream
import solutions.cvs.sdk.randomString


/// Local camera/mic stream
class CameraStream(
    camera: Camera?,
    sreamId: String?,
    val audio: AudioConstraints?,
    val video: VideoConstraints?
) : Stream {

    private lateinit var videoCapturer: VideoCapturer
    private lateinit var surfaceTextureHelper: SurfaceTextureHelper
    private lateinit var streamView: SurfaceViewRenderer

    override lateinit var stream: MediaStream
        private set

    override val id: String;

    override val view: SurfaceView
        get() = streamView

    init {
        id = sreamId ?: randomString(8)
        stream = Package.peerConnectionFactory.createLocalMediaStream(id)
        audio?.let {
            val audioSource = Package.peerConnectionFactory.createAudioSource(it)
            val audioTrack = Package.peerConnectionFactory.createAudioTrack("$id-audio", audioSource)
            stream.addTrack(audioTrack)
        }
        (camera ?: Camera.default)?.let { itCamera ->
            video?.let {
                videoCapturer = itCamera.createCapturer() ?: throw Error("Cannot create camera capturer")
                val eglBaseContext = Package.eglBase.eglBaseContext
                surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBaseContext)
                val videoSource = Package.peerConnectionFactory.createVideoSource(videoCapturer.isScreencast)
                videoCapturer.initialize(surfaceTextureHelper, Package.applicationContext, videoSource.capturerObserver)
                videoCapturer.startCapture(it.width, it.height, it.frameRate)
                val videoTrack = Package.peerConnectionFactory.createVideoTrack("$id-video", videoSource)
                stream.addTrack(videoTrack)
                streamView = SurfaceViewRenderer(Package.applicationContext)
                streamView.init(eglBaseContext, null)
                streamView.setMirror(true)
                streamView.setEnableHardwareScaler(true)
                streamView.setZOrderMediaOverlay(true)
                videoTrack.addSink(streamView)
            }
        }
    }

    override fun close() {
        if (this::videoCapturer.isInitialized) videoCapturer.dispose()
        if (this::surfaceTextureHelper.isInitialized) surfaceTextureHelper.dispose()
        if (this::streamView.isInitialized) {
            if (!stream.videoTracks.isEmpty())
                stream.videoTracks[0].removeSink(streamView)
            streamView.clearImage()
            streamView.release()
        }
        stream.dispose()
    }
}