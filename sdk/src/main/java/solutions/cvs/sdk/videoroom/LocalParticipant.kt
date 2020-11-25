package solutions.cvs.sdk.videoroom

import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.PeerConnection
import solutions.cvs.sdk.janus.Participant
import solutions.cvs.sdk.janus.Plugin


/// Local participant
class LocalParticipant() : Participant("janus.plugin.videoroom"), Plugin {

    override fun onIceCandidate(p0: IceCandidate?) {
        super.onIceCandidate(p0)
        sendMessage(
            JSONObject(
                mapOf("janus" to "trickle", "candidate" to p0.toString())
            )
        )
    }

    override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
        super.onIceGatheringChange(p0)
        p0?.let {
            if (it.toString() == "CONNECTED") {
                sendMessage(
                    JSONObject(
                        mapOf("janus" to "trickle", "candidate" to JSONObject(mapOf("completed" to true)))
                    )
                )
            }
        }
    }


}