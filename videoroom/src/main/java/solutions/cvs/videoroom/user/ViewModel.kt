package solutions.cvs.videoroom.user

import android.util.Log
import androidx.lifecycle.ViewModel

class ViewModel : ViewModel() {

    fun doLogin(username: String, password: String) {
        Log.i("USER", "doLogin: ${username}, ${password}")
    }
}