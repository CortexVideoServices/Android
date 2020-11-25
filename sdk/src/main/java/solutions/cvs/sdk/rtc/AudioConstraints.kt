package solutions.cvs.sdk.rtc

import org.webrtc.MediaConstraints


/// Audio constraints
class AudioConstraints(val audioProcessing: Boolean = true)  : MediaConstraints() {
    init {
        mandatory.addAll(
            listOf(
                MediaConstraints.KeyValuePair("googEchoCancellation", audioProcessing.toString()),
                MediaConstraints.KeyValuePair("googAutoGainControl", audioProcessing.toString()),
                MediaConstraints.KeyValuePair("googHighpassFilter", audioProcessing.toString()),
                MediaConstraints.KeyValuePair("googNoiseSuppression", audioProcessing.toString())
            )
        )
    }
}