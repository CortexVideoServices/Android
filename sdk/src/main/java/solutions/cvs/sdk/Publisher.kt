package solutions.cvs.sdk

import android.content.Context
import org.webrtc.EglBase
import org.webrtc.VideoCapturer
import solutions.cvs.sdk.rtc.AudioConstraints
import solutions.cvs.sdk.rtc.VideoConstraints

interface Publisher : Stream {

    /// Starts capturer
    fun startCapturer(): Unit

    /// Stop capturer
    fun stopCapturer(): Unit

    /// Session settings
    interface Settings : Session.Settings {

        /// Participant name
        val participantName: String?

        /// Capturer
        val videoCapturer: VideoCapturer?

        /// Audio constraints
        val audioConstraints: AudioConstraints?

        /// Video constraints
        val videoConstraints: VideoConstraints?
    }

    /// Session observer
    interface Observer {
        /// Called when an error occurred
        fun onError(reason: Throwable): Unit?

        /// Called when a participant's stream has been created
        fun onStreamCreated(stream: Stream): Unit?

        /// Called before a participant's stream is destroyed
        fun onStreamDestroy(stream: Stream): Unit?
    }
}