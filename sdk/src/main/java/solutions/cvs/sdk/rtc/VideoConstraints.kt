package solutions.cvs.sdk.rtc

import org.webrtc.MediaConstraints


/// Video constraints
class VideoConstraints(
    val frameResolution: Quality = Quality.Medium,
    val frameRate: Quality = Quality.Medium,
    val frameWide: Boolean = false
) : MediaConstraints() {

    /// Quality enum
    enum class Quality(val value: Int) {
        Low(-1),
        Medium(0),
        High(1),
    }

    /// Frame width
    val width: Int
        get() {
            if (frameWide)
                return if (frameResolution.value >= 0) if (frameResolution.value > 0) 1280 else 720 else 352
            else
                return if (frameResolution.value >= 0) if (frameResolution.value > 0) 1280 else 640 else 320
        }

    /// Frame height
    val height: Int
        get() {
            if (frameWide)
                return if (frameResolution.value >= 0) if (frameResolution.value > 0) 720 else 480 else 288
            else
                return if (frameResolution.value >= 0) if (frameResolution.value > 0) 960 else 480 else 240
        }

    /// Frame rate
    val rate: Int
        get() = if (frameRate.value >= 0) if (frameRate.value > 0) 30 else 15 else 7

    init {
        mandatory.add(KeyValuePair("maxWidth", width.toString()))
        mandatory.add(KeyValuePair("maxHeight", height.toString()))
        mandatory.add(KeyValuePair("FrameRate", rate.toString()))
    }
}