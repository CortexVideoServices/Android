package solutions.cvs.videoroom.user

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.google.android.material.snackbar.Snackbar


/**
 * User data
 */
data class UserData(val email: String, val display_name: String)

/**
 * User session
 */
class UserSession(application: Application) : AndroidViewModel(application) {

    /**
     * Current user data if this session is authenticated, otherwise null
     */
    val user: LiveData<UserData?>
        get() = liveUserData

    /**
     * True if session authenticated
     */
    val authenticated: LiveData<Boolean>
        get() = Transformations.map(liveUserData, { userData -> userData != null })

    /**
     * Last user session error.
     */
    val error: LiveData<String?>
        get() = liveError

    /**
     * Resets last session error
     */
    fun resetError() {
        liveError.value = null;
    }

    /**
     * Tries to login with given username and password
     */
    fun doLogin(username: String?, password: String?) {
        // ToDo:  Should be implemented
        if (username != null && password == "123456") {
            setUserData(UserData(username, username))
        } else liveError.value = "Incorrect username or password"
    }

    /**
     * Logs out user session
     */
    fun doLogout() {
        // ToDo:  Should be implemented
        resetUserData()
    }

    private lateinit var sPref: SharedPreferences
    private val liveUserData = MutableLiveData<UserData?>()
    private val liveError = MutableLiveData<String?>()

    init {
        sPref = application.applicationContext.getSharedPreferences("user_data", Context.MODE_PRIVATE)
        val userData: UserData? = sPref.run {
            val email = getString("email", null)
            val display_name = getString("display_name", null)
            if (email != null && display_name != null) UserData(email, display_name)
            else null
        }
        if (userData != null)
            liveUserData.value = userData
    }

    private fun setUserData(userData: UserData) {
        sPref.edit().run {
            putString("email", userData.email)
            putString("display_name", userData.display_name)
            apply()
        }
        if (!(liveUserData.value != null && liveUserData.value!!.email == userData.email))
            liveUserData.value = userData
    }

    private fun resetUserData() {
        if (liveUserData.value != null)
            liveUserData.value = sPref.edit().run {
                putString("email", null)
                putString("display_name", null)
                apply()
                null
            }
    }

    override fun onCleared() {
        super.onCleared()
    }
}


