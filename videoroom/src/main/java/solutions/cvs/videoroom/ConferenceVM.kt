package solutions.cvs.videoroom

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import android.view.View
import androidx.core.os.bundleOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.Transformations.*
import androidx.navigation.findNavController
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import solutions.cvs.videoroom.api.Client
import solutions.cvs.videoroom.api.ConferenceData


class ConferenceVM(application: Application) : AndroidViewModel(application) {

    /**
     * Current (selected) conference
     */
    val conferenceData: LiveData<ConferenceData?>
        get() = liveConferenceData


    val displayName: LiveData<String>
        get() = map(liveConferenceData) { conferenceData -> conferenceData?.displayName ?: "" }

    val description: LiveData<String>
        get() = map(liveConferenceData) { conferenceData -> conferenceData?.description ?: "" }

    val allowAnonymous: LiveData<Boolean>
        get() = map(liveConferenceData) { conferenceData -> conferenceData?.allowAnonymous ?: false }

    /**
     * Conference data ready
     */
    val conferenceReady: LiveData<Boolean>
        get() = liveConferenceReady

    private var viewModelJob = Job()
    private val viewModelScope = CoroutineScope(Dispatchers.Main + viewModelJob)
    private val sPref = application.applicationContext.getSharedPreferences("videoroom", Context.MODE_PRIVATE)
    private val baseUrl = sPref.getString("baseUrl", "https://cvs.solutions") ?: ""
    private var client: Client = Client.instance(baseUrl)
    private val liveError = MutableLiveData<String?>()
    private val liveConferenceData = MutableLiveData<ConferenceData?>()
    private val liveConferenceReady = MutableLiveData<Boolean>()

    fun createConference(displayName: String?, description: String?, allowAnonymous: Boolean?) {
        if (displayName != null) {
            viewModelScope.launch {
                try {
                    val conferenceData =
                        client.createConference(displayName, description ?: "", allowAnonymous ?: false)
                    liveConferenceData.value = conferenceData;
                    if (conferenceData == null) liveError.value = "Wrong or outdated session"
                } catch (e: Throwable) {
                    val message = "Cannot received data from server"
                    liveError.value = message
                    Log.e("CAUGHT", message, e)
                }
            }
        }
    }

    fun currentConference() {
        viewModelScope.launch {
            try {
                val conferenceData = client.currentConference()
                liveConferenceData.value = conferenceData;
                if (conferenceData == null) liveError.value = "Wrong or outdated session"
                liveConferenceReady.value = true;
            } catch (e: Throwable) {
                val message = "Cannot received data from server"
                liveError.value = message
                Log.e("CAUGHT", message, e)
            }
        }
    }

    fun copyToClipboard(view: View) {
        val sessionId = conferenceData.value!!.sessionId;

        val clipboardManager = view.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val url = "$baseUrl/#/conference/${sessionId}"
        val data = ClipData.newPlainText("url", url)
        Snackbar.make(view, R.string.session_to_clipboard, Snackbar.LENGTH_LONG)
            .setAction("OK") {
                clipboardManager.setPrimaryClip(data)
            }.show();
    }

    fun startConference(view: View) {
        val sessionId = conferenceData.value!!.sessionId;
        val bundle = bundleOf("sessionId" to sessionId)
        view.findNavController().navigate(R.id.action_2VideoRoom, bundle);
    }
}