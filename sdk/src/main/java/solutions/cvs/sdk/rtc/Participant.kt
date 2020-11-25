package solutions.cvs.sdk.rtc

import android.util.Log
import org.webrtc.*
import solutions.cvs.sdk.Connection
import solutions.cvs.sdk.SDKError
import solutions.cvs.sdk.Session
import solutions.cvs.sdk.Stream
import solutions.cvs.sdk.utl.PeerConnectionObserver
import java.util.concurrent.Executors

/// Abstract base RTC participant class
abstract class Participant(
    private val settings: Session.Settings,
    logger: ((message: String) -> Unit)?
) : PeerConnectionObserver(logger), Stream {

    protected class ProxyVideoSink : VideoSink {
        private var target: VideoSink? = null

        @Synchronized
        override fun onFrame(frame: VideoFrame) {
            if (target == null) {
                Log.d("SDK/DEBUG", "Dropping frame in proxy because target is null.")
                return
            }
            target!!.onFrame(frame)
        }

        @Synchronized
        fun setTarget(target: VideoSink?) {
            this.target = target
        }
    }

    /// Participant id
    override val id = randomString(7)


    private val singleThreadExecutor = Executors.newSingleThreadExecutor()

    interface ThisThread {
        fun post(run: () -> Unit): Unit
    }

    /// Post execution to participant thread
    protected val thisThread = object : ThisThread {
        override fun post(run: () -> Unit) {
            singleThreadExecutor.execute {
                run()
            }
        }
    }

    /// Peer connection factory
    protected var peerConnection: PeerConnection? = null
        get() {
            if (field == null)
                field = factory.createPeerConnection(settings.rtcConfiguration, this)
            return field
        }
        set(_) {
            try {
                field?.close()
            } catch (_: Exception) {
            } finally {
                field = null
            }
        }

    /// Starts peer connection
    protected open fun startPeer() {
        val peerConnection = this.peerConnection ?: throw SDKError("Cannot create peer connection")
    }

    /// Stops peer connection
    protected fun stopPeer() {
        peerConnection = null
    }

    /// Returns a random string [0-9,A-Z] with the given size
    protected fun randomString(size: Int): String {
        val alphabet: List<Char> = ('A'..'Z') + ('0'..'9')
        return List(size) { alphabet.random() }.joinToString("")
    }

    private lateinit var safeThread: Thread

    /// Peer connection factory
    protected val factory: PeerConnectionFactory
        get() {
            if (this::safeThread.isInitialized && Thread.currentThread() !== safeThread)
                throw IllegalThreadStateException(
                    "Peer factory can only be used on the thread in which it was created."
                )
            return internalFactory
        }

    private val internalFactory by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        safeThread = Thread.currentThread()
        val encoderFactory: VideoEncoderFactory
        val decoderFactory: VideoDecoderFactory
        if (settings.hardwareAcceleration) {
            val rootEglBase = settings.eglBase
            encoderFactory = DefaultVideoEncoderFactory(
                rootEglBase.eglBaseContext,
                true, false // ToDo: H264HighProfile
            )
            decoderFactory = DefaultVideoDecoderFactory(rootEglBase.eglBaseContext)
        } else {
            encoderFactory = SoftwareVideoEncoderFactory()
            decoderFactory = SoftwareVideoDecoderFactory()
        }
        PeerConnectionFactory.builder()
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory()
    }

    /// Makes SdpObserver
    protected fun makeSdpObserver(onResult: (result: Any) -> Unit): SdpObserver {
        return object : SdpObserver {
            override fun onSetSuccess() = onResult(true)
            override fun onSetFailure(p0: String?) = onResult(SDKError(p0))
            override fun onCreateFailure(p0: String?) = onResult(SDKError(p0))
            override fun onCreateSuccess(p0: SessionDescription) = onResult(p0)
        }
    }

    /// Create offer
    protected fun createOffer(audio: Boolean, video: Boolean, onResult: (result: Any) -> Unit) {
        thisThread.post {
            val makeError: (Throwable?) -> Exception =
                { reason -> SDKError("Cannot create offer", reason) }
            val offerConstraints = MediaConstraints().apply {
                mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", audio.toString()))
                mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", video.toString()))
            }
            if (peerConnection == null)
                onResult(makeError(SDKError("Peer not connected")))

            peerConnection?.run {
                this.createOffer(makeSdpObserver { sdrResult ->
                    if (sdrResult is SessionDescription) {
                        setLocalDescription(makeSdpObserver { result ->
                            if (result is Throwable) onResult(result)
                            else onResult(sdrResult)
                        }, sdrResult)
                    } else onResult(makeError(if (sdrResult is Throwable) sdrResult else null))
                }, offerConstraints)
            }
        }
    }

    /// Applies answer
    protected fun applyAnswer(sdr: SessionDescription, onResult: (result: Any) -> Unit) {
        thisThread.post {
            val makeError: (Throwable?) -> Exception =
                { reason -> SDKError("Cannot apply answer", reason) }
            if (peerConnection == null)
                onResult(makeError(SDKError("Peer not connected")))

            peerConnection?.run {
                this.setRemoteDescription(makeSdpObserver { result ->
                    if (result is Throwable) onResult(result)
                    else onResult(true)
                }, sdr)
            }
        }
    }

    /// Applies a remote offer
    protected fun applyOffer(remoteOffer: SessionDescription, onResult: (result: Any) -> Unit): Unit {
        thisThread.post {
            val makeError: (Throwable?) -> Exception =
                { reason -> SDKError("Cannot apply offer", reason) }
            if (peerConnection == null)
                onResult(makeError(SDKError("Peer not connected")))

            peerConnection?.run {
                setRemoteDescription(makeSdpObserver { result ->
                    if (result is Throwable) onResult(makeError(result))
                    else onResult(true)
                }, remoteOffer)
            }
        }
    }

    /// Creates answer for a remote offer
    protected fun createAnswer(onResult: (result: Any) -> Unit): Unit {
        thisThread.post {
            val makeError: (Throwable?) -> Exception =
                { reason -> SDKError("Cannot create answer", reason) }
            val answerConstraints = MediaConstraints().apply {
                mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
                mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
            }

            if (peerConnection == null)
                onResult(makeError(SDKError("Peer not connected")))

            peerConnection?.run {
                createAnswer(makeSdpObserver { sdrResult ->
                    if (sdrResult is SessionDescription) {
                        setLocalDescription(makeSdpObserver { result ->
                            if (result is Throwable) onResult(makeError(result))
                            else onResult(sdrResult)
                        }, sdrResult)
                    } else
                        onResult(makeError(if (sdrResult is Throwable) sdrResult else null))
                }, answerConstraints)
            }
        }
    }
}