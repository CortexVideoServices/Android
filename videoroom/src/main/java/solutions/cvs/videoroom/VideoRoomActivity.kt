package solutions.cvs.videoroom

import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setMargins
import com.google.android.flexbox.FlexboxLayout
import kotlinx.android.synthetic.main.activity_videoroom.*
import solutions.cvs.sdk.*
import java.net.URL


/**
 * Video room activity
 */
class VideoRoomActivity : AppCompatActivity() {

    private lateinit var session: Session
    private lateinit var publisher: Publisher


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_videoroom)

        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.setSpeakerphoneOn(true)
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION)

        val logger = { message: String -> this.logger(message) }
        val sPref = application.applicationContext.getSharedPreferences("videoroom", Context.MODE_PRIVATE)
        val baseUrl = URL(sPref.getString("baseUrl", "https://cvs.solutions") ?: "")
        val protocol = if (baseUrl.protocol == "https") "wss" else "ws"
        val port = if (baseUrl.port>0) ":${baseUrl.port}" else ""
        val serverUrl = "${protocol}://${baseUrl.host}:${port}/cvs/ws/v1"
        val sessionId = intent.data.toString();

        session = SessionBuilder(applicationContext, serverUrl, sessionId)
            .apply {
                debug = true
            }
            .build(
                SessionObserver(
                    onConnected = { session.publish(publisher) },
                    onDisconnected = { session.unpublish(publisher) },
                    onStreamReceived = { stream -> addStreamView(stream) },
                    onStreamDropped = { stream -> removeStreamView(stream) },
                    logger = logger
                )
            )
    }

    override fun onStart() {
        super.onStart()
        val logger = { message: String -> this.logger(message) }
        publisher = PublisherBuilder(session)
            .build(
                PublisherObserver(
                    onStreamCreated = { stream -> addStreamView(stream) },
                    onStreamDestroy = { stream -> removeStreamView(stream) },
                    logger = logger
                )
            )
        session.connect()
    }

    override fun onStop() {
        super.onStop()
        session.disconnect()
        publisher.destroy()
    }

    private fun pxFromDp(dp: Int): Int {
        return (dp * applicationContext.getResources().getDisplayMetrics().density).toInt()
    }

    var streamViews = mutableSetOf<View>()


    private fun addStreamView(stream: Stream) {
        stream.view?.let { view->
            if (streamViews.add(view))
                updateViews(streamViews.toList())
        }

    }

    private fun removeStreamView(stream: Stream) {
        stream.view?.let { view ->
            if (streamViews.remove(view))
                updateViews(streamViews.toList())
        }
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


    private fun logger(message: String) {
        Log.d("SDK/LOGGER", message)
    }
}