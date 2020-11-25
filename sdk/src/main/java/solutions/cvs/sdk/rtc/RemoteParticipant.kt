package solutions.cvs.sdk.rtc

import android.os.Handler
import android.os.Looper
import android.view.SurfaceView
import org.webrtc.MediaStream
import org.webrtc.SurfaceViewRenderer
import solutions.cvs.sdk.Session


/// Remote participant (received stream)
abstract class RemoteParticipant(
    private var settings: Session.Settings,
    logger: ((message: String) -> Unit)? = null
) : Participant(
    settings,
    logger
) {

    protected var mediaStream: MediaStream? = null
        private set

    private var streamView: SurfaceViewRenderer? = null
    override val view: SurfaceView?
        get() = streamView


    override var audio: Boolean = true
    override var video: Boolean = false

    protected var streamAwaiter: ((stream: MediaStream) -> Unit)? = null
    protected fun awaitStream(onResult: (Any?) -> Unit) {
        if (mediaStream != null) onResult(mediaStream)
        else streamAwaiter = { stream -> onResult(stream) }
    }

    override fun onAddStream(mediaStream: MediaStream) {
        super.onAddStream(mediaStream)
        mediaStream.let { stream ->
            this.mediaStream = stream
            createView()

            streamAwaiter?.let { callback ->
                callback(stream)
            }
        }
    }

    private val mainThread = Handler(Looper.getMainLooper())
    private val streamViewProxy = ProxyVideoSink()

    override fun destroy() {
        streamView?.let { view->
            streamViewProxy.setTarget(null)
            mainThread.post {
                view.clearImage()
                view.release()
            }
        }
        mediaStream?.dispose()
    }

    private fun createView() {
        mediaStream?.run {
            if (!videoTracks.isEmpty()) {
                video = true
                val videoTrack = videoTracks.get(0)
                mainThread.post {
                    streamView = SurfaceViewRenderer(settings.appContext).apply {
                        init(settings.eglBase.eglBaseContext, null)
                        setEnableHardwareScaler(true)
                        setZOrderMediaOverlay(true)
                    }
                    streamViewProxy.setTarget(streamView)
                    videoTrack.addSink(streamViewProxy)
                }
            }
        }
    }
}