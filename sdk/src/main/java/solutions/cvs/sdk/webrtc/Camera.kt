package solutions.cvs.sdk.webrtc

import android.content.Context
import android.util.ArraySet
import org.webrtc.Camera2Enumerator
import org.webrtc.VideoCapturer

/// Camera
class Camera(val deviceId: String, val title: String, val frontFacing: Boolean) {

    companion object {
        private val cameraSet = ArraySet<Camera>()
        private lateinit var applicationContext: Context

        /// Returns camera list
        val list: List<Camera>
            get() = cameraSet.toList()

        /// Returns default camera
        val default: Camera?
            get() = list.filter { it.frontFacing }.getOrElse(0) { list.firstOrNull() }

        /// Initializes camera
        fun init(applicationContext: Context) {
            this.applicationContext = applicationContext
            val enumerator = Camera2Enumerator(applicationContext)
            for (deviceId in enumerator.deviceNames) {
                if (enumerator.isFrontFacing(deviceId))
                    cameraSet.add(Camera(deviceId, "Front#${deviceId}", true))
                else
                    cameraSet.add(Camera(deviceId, "Back#${deviceId}", false))
            }
        }

    }

    /// Create camera capturer
    fun createCapturer(): VideoCapturer? {
        val enumerator = Camera2Enumerator(applicationContext)
        return enumerator.createCapturer(deviceId, null)
    }
}