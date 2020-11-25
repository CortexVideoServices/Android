package solutions.cvs.sdk

import android.view.SurfaceView

/// Stream interface
interface Stream {

    /// ID
    val id: String

    /// Participant name
    val participantName: String?

    /// View where rendering stream
    val view: SurfaceView?

    /// Audio enable/disable switcher
    var audio: Boolean

    /// Video enable/disable switcher
    var video: Boolean

    /// Closes and destructs stream
    fun destroy(): Unit

}