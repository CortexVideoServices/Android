package solutions.cvs.videoroom


import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import solutions.cvs.videoroom.api.Client
import solutions.cvs.videoroom.api.UserData


/**
 * User session
 */
class UserSession(application: Application) : AndroidViewModel(application) {

    private var viewModelJob = Job()
    private val sPref = application.applicationContext.getSharedPreferences("videoroom", Context.MODE_PRIVATE)
    private val viewModelScope = CoroutineScope(Dispatchers.Main + viewModelJob)
    private val liveError = MutableLiveData<String?>()
    private var client: Client = Client.instance(sPref.getString("baseUrl", "https://cvs.solutions") ?:"")

    /**
     * Current user data if this session is authenticated, otherwise null
     */
    val userData: LiveData<UserData?>
        get() = Transformations.map(client.sessionData) { sessionData -> sessionData?.userData }

    /**
     * True if session authenticated
     */
    val authenticated: LiveData<Boolean>
        get() = Transformations.map(userData, { userData -> userData != null })

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
        if (username != null && password != null) {
            viewModelScope.launch {
                try {
                    val userData = client.login(username, password)
                    if (userData == null) liveError.value = "Incorrect username or password"
                } catch (e: Throwable) {
                    val message = "Cannot received data from server"
                    liveError.value = message
                    Log.e("CAUGHT", message, e)
                }
            }
        }
    }

    /**
     * Logs out user session
     */
    fun doLogout() {
        // ToDo:  Should be implemented
        viewModelScope.launch {
            try {
                client.logoff()
            } catch (e: Throwable) {
                Log.e("CAUGHT", "Error while logout", e)
            }
        }
    }
}


