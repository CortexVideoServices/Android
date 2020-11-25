package solutions.cvs.sdk.webrtc

import org.webrtc.MediaConstraints


/// Quality enum
enum class Quality(val value: Int) {
    Low(-1),
    Medium(0),
    High(1),
}


/// Audio constraints
class AudioConstraints(
    val quality: Quality = Quality.Medium
) : MediaConstraints() {}


/// Video constraints
class VideoConstraints(
    val quality: Quality = Quality.Low,
    val frameRate: Int = 15,
    val wide: Boolean = false
) : MediaConstraints() {

    /// Frame width
    val width: Int
        get() {
            if (wide)
                return if (quality.value >= 0) if (quality.value > 0) 1280 else 720 else 352
            else
                return if (quality.value >= 0) if (quality.value > 0) 1280 else 640 else 320
        }

    /// Frame height
    val height: Int
        get() {
            if (wide)
                return if (quality.value >= 0) if (quality.value > 0) 720 else 480 else 288
            else
                return if (quality.value >= 0) if (quality.value > 0) 960 else 480 else 240
        }

    init {
        mandatory.add(MediaConstraints.KeyValuePair("maxWidth", width.toString()));
        mandatory.add(MediaConstraints.KeyValuePair("maxHeight", height.toString()));
        mandatory.add(MediaConstraints.KeyValuePair("FrameRate", frameRate.toString()));
    }
}