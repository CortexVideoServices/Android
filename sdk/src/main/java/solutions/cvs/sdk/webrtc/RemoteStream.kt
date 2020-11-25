package solutions.cvs.sdk.webrtc

import android.os.Handler
import android.view.View
import android.widget.ImageView
import org.webrtc.*
import solutions.cvs.sdk.Package
import solutions.cvs.sdk.R
import solutions.cvs.sdk.Stream

class RemoteStream(
    override val stream: MediaStream
) : Stream {
    private val eglBase = EglBase.create()
    private var streamView: SurfaceViewRenderer? = null

    override val id: String
        get() = stream.id

    override val view: View
        get() {
            return streamView
                ?: ImageView(Package.applicationContext).apply { setImageResource(R.drawable.videocam_off_24) };
        }

    private val mainHandler = Handler(Package.applicationContext.mainLooper);
    private val proxyVideoSink = ProxyVideoSink()

    init {
        if (!stream.videoTracks.isEmpty()) {
            val videoTrack = stream.videoTracks.get(0)
            //mainHandler.post {
            streamView = SurfaceViewRenderer(Package.applicationContext).apply {
                init(eglBase.eglBaseContext, null)
                setEnableHardwareScaler(true)
                setZOrderMediaOverlay(true)
            }
            proxyVideoSink.setTarget(streamView)
            videoTrack.addSink(proxyVideoSink)
            //}
        }
    }

    override fun close() {
        streamView?.let { view ->
            proxyVideoSink.setTarget(null)
            mainHandler.post {
                view.clearImage()
                view.release()
            }
        }
        stream.dispose()
    }

    private class ProxyVideoSink : VideoSink {
        private var target: VideoSink? = null

        @Synchronized
        override fun onFrame(frame: VideoFrame) {
            if (target == null) {
                Logging.d(
                    "SDK/PROXY",
                    "Dropping frame in proxy because target is null."
                )
                return
            }
            target!!.onFrame(frame)
        }

        @Synchronized
        fun setTarget(target: VideoSink?) {
            this.target = target
        }
    }
}