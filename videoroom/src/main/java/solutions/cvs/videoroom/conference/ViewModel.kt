package solutions.cvs.videoroom.conference

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel


class ViewModel(application: Application) : AndroidViewModel(application) {

    fun createConference(displayName: String?, description: String?, allowAnonymous: Boolean) {
        // ToDo:
        Log.i("USER", "createConference: ${displayName}, ${description}, ${allowAnonymous}")
    }
}