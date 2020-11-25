package solutions.cvs.videoroom

import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setMargins
import com.google.android.flexbox.FlexboxLayout
import kotlinx.android.synthetic.main.activity_videoroom.*
import solutions.cvs.sdk.janus.Session
import solutions.cvs.sdk.Publisher
import solutions.cvs.sdk.PublisherListener
import solutions.cvs.sdk.Stream
import solutions.cvs.sdk.janus.SessionListener
import java.net.URL
import solutions.cvs.sdk.Package as SDK

/**
 * Video room activity
 */
class VideoRoomActivity : AppCompatActivity() {

    private lateinit var session: Session
    private lateinit var publisher: Publisher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_videoroom)

        val sPref = application.applicationContext.getSharedPreferences("videoroom", Context.MODE_PRIVATE)
        val baseUrl = URL(sPref.getString("baseUrl", "https://cvs.solutions") ?: "")
        val protocol = if (baseUrl.protocol == "https") "wss" else "ws"
        val port = if (baseUrl.port>0) ":${baseUrl.port}" else ""
        val serverUrl = "${protocol}://${baseUrl.host}:${port}/cvs/ws/v1"
        SDK.init(applicationContext, serverUrl, true) // ToDo: remove debug

        val sessionID = intent.data.toString();
        session = solutions.cvs.sdk.Session.create(sessionID)
        session.addListener(object : SessionListener {
            override fun onConnected(session: solutions.cvs.sdk.janus.Session) {
                val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                audioManager.setSpeakerphoneOn(true)
                audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION)
                publisher.stream?.let { stream -> session.publishStream(stream, "Anonymous") }
            }
            override fun onStreamCreated(session: Session, stream: Stream) = addStreamView(stream)
            override fun onStreamDestroy(session: Session, stream: Stream) = removeStreamView(stream)
        })
        publisher = Publisher.create()
        publisher.addListener(object : PublisherListener {
            override fun onStreamCreated(publisher: Publisher, stream: Stream) = addStreamView(stream)
            override fun onStreamDestroy(publisher: Publisher, stream: Stream) = removeStreamView(stream)
            override fun onError(publisher: Publisher, reason: Throwable) = this@VideoRoomActivity.onError(publisher, reason)
        })
    }

    override fun onStart() {
        super.onStart()
        if (this::session.isInitialized)
            session.connect()
        if (this::publisher.isInitialized)
            publisher.startCapturing()
    }

    override fun onStop() {
        if (this::publisher.isInitialized)
            publisher.stopCapturing()
        if (this::session.isInitialized)
            session.disconnect()
        super.onStop()
    }


    private fun pxFromDp(dp: Int): Int {
        return (dp * applicationContext.getResources().getDisplayMetrics().density).toInt()
    }

    var streamViews = mutableSetOf<View>()

    @UiThread
    private fun addStreamView(stream: Stream) {
        if (streamViews.add(stream.view))
            updateViews(streamViews.toList())
    }

    @UiThread
    private fun removeStreamView(stream: Stream) {
        if (streamViews.remove(stream.view))
            updateViews(streamViews.toList())
    }

    private fun updateViews(views: List<View>) {
        placeMainVideo.removeAllViews()
        flexLayout.removeAllViews()
        flexLayout.refreshDrawableState()
        if (views.size > 0) {
            val lp = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            views[0].layoutParams = lp
            placeMainVideo.addView(views[0])
        }
        if (views.size > 1) {
            val lp = FlexboxLayout.LayoutParams(pxFromDp(160), pxFromDp(120))
            lp.setMargins(pxFromDp(8))
            for (i in 1..views.size-1) {
                views[i].layoutParams = lp
                flexLayout.addView(views[i])
                views[i].setOnClickListener {
                    val tempSet = mutableSetOf<View>()
                    tempSet.add(views[i])
                    streamViews.filter { view -> view != views[i] }.forEach{view->tempSet.add(view)}
                    streamViews = tempSet
                    updateViews(streamViews.toList())
                }
            }
        }
    }

    private fun onError(sender: Any, reason: Throwable) {
        Log.e("SDK/SAMPLE", "${sender.toString()}; Error: ${reason.toString()}" )
    }
}