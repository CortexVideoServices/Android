package solutions.cvs.sdk.rtc

import android.content.Context
import org.webrtc.Camera1Enumerator
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraEnumerator
import org.webrtc.VideoCapturer


/// Camera
object Camera {

    /// Camera info
    data class Info(val deviceId: String, val title: String, val frontFacing: Boolean)

    private val cameraSet = mutableSetOf<Info>()

    /// Returns camera list
    val list: List<Info>
        get() = cameraSet.toList()

    /// Returns default camera
    val default: Info?
        get() = list.filter { it.frontFacing }.getOrElse(0) { list.firstOrNull() }

    private lateinit var enumerator: CameraEnumerator

    internal fun initialize(applicationContext: Context) {
        if (Camera2Enumerator.isSupported(applicationContext))
            enumerator = Camera2Enumerator(applicationContext)
        else
            enumerator = Camera1Enumerator(true)
        updateCameraList()
    }

    /// Updates camera list
    fun updateCameraList() {
        cameraSet.clear()
        for (deviceId in enumerator.deviceNames) {
            if (enumerator.isFrontFacing(deviceId))
                cameraSet.add(Info(deviceId, "Front#${deviceId}", true))
            else
                cameraSet.add(Info(deviceId, "Back#${deviceId}", false))
        }
    }

    /// Create camera capturer
    fun createCapturer(deviceId: String?): VideoCapturer? {
        val id = deviceId ?: default?.deviceId
        id?.run {
            return@createCapturer enumerator.createCapturer(id, null)
        }
        return null
    }
}