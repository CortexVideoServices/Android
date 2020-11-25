package solutions.cvs.sdk

import android.util.ArraySet
import android.util.Log
import solutions.cvs.sdk.webrtc.AudioConstraints
import solutions.cvs.sdk.webrtc.Camera
import solutions.cvs.sdk.webrtc.CameraStream
import solutions.cvs.sdk.webrtc.VideoConstraints
import java.io.PrintWriter
import java.io.StringWriter


/// Local stream publisher interface
interface PublisherListener {

    /// Called when the publisher's stream has created.
    fun onStreamCreated(publisher: Publisher, stream: Stream) {}

    /// Called before the publisher stream is destroyed.
    fun onStreamDestroy(publisher: Publisher, stream: Stream) {}

    /// Called when a publisher fails.
    fun onError(publisher: Publisher, reason: Throwable) {}
}

/// Local stream publisher
interface Publisher {

    /// Publisher stream if not null
    val stream: Stream?

    /// Selected audio constraints
    var audio: AudioConstraints

    /// Selected video constraints
    var video: VideoConstraints

    /// Camera list (device ID, title), the first item should be the default front camera
    val cameraList: List<Camera>

    /// Prepares and starts capturing the local stream.
    fun startCapturing(audio: Boolean = true, video: Boolean = true): Unit

    /// Stops capturing the local stream and dispose it.
    fun stopCapturing(): Unit

    /// Adds publisher listener
    fun addListener(listener: PublisherListener): PublisherListener

    /// Removes publisher listener
    fun removeListener(listener: PublisherListener): PublisherListener

    companion object {
        /// Creates publisher
        fun create(
            audio: AudioConstraints = AudioConstraints(),
            video: VideoConstraints = VideoConstraints()
        ): Publisher {
            return PublisherImpl(audio, video)
        }
    }

}

internal class PublisherImpl(
    override var audio: AudioConstraints = AudioConstraints(),
    override var video: VideoConstraints = VideoConstraints()
) : Publisher {

    override lateinit var cameraList: List<Camera>
        private set

    override var stream: Stream? = null
        private set
    private var currentCamera: Camera? = null
    private val listeners = ArraySet<PublisherListener>()

    final override fun addListener(listener: PublisherListener) = listener.apply { listeners.add(listener) }
    final override fun removeListener(listener: PublisherListener) = listener.apply { listeners.remove(listener) }

    init {
        currentCamera = Camera.default
        cameraList = Camera.list
        if (Package.debug) {
            addListener(object : PublisherListener {
                override fun onStreamCreated(publisher: Publisher, stream: Stream) {
                    Log.d("SDK/EVENTS", "$publisher; onStreamCreated: $stream")
                }

                override fun onStreamDestroy(publisher: Publisher, stream: Stream) {
                    Log.d("SDK/EVENTS", "$publisher; onStreamDestroy: $stream")
                }

                override fun onError(publisher: Publisher, reason: Throwable) {
                    val stackTrace = StringWriter()
                    reason.printStackTrace(PrintWriter(stackTrace))
                    Log.d("SDK/EVENTS", "$publisher; onError: $reason\n$stackTrace")
                }
            })
        }
    }

    override fun startCapturing(audio: Boolean, video: Boolean) {
        stopCapturing()
        try {
            stream = CameraStream(
                currentCamera, null, if (audio) this.audio else null, if (video) this.video else null
            ).also { stream ->
                listeners.forEach { it.onStreamCreated(this, stream) }
            }
        } catch (reason: Error) {
            listeners.forEach { it.onError(this, Error("Cannot start capturing", reason)) }
            stopCapturing()
        }

    }

    override fun stopCapturing() {
        stream?.let { stream ->
            listeners.forEach { it.onStreamDestroy(this, stream) }
            stream.close()
        }
        stream = null
    }
}