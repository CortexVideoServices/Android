package solutions.cvs.videoroom

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel


class ConferenceVM(application: Application) : AndroidViewModel(application) {

    fun createConference(displayName: String?, description: String?, allowAnonymous: Boolean) {
        // ToDo:
        Log.i("USER", "createConference: ${displayName}, ${description}, ${allowAnonymous}")
    }
}