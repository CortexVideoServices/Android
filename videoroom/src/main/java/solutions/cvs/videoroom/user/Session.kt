package solutions.cvs.videoroom.user

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences


/**
 * User data
 */
data class UserData(val email: String, val display_name: String)

/**
 * User session
 */
class Session {
    private lateinit var sPref: SharedPreferences

    fun Session(context: Context) {
        sPref = context.applicationContext.getSharedPreferences("user_data", MODE_PRIVATE)
    }

    /**
     * Return true if session authenticated
     */
    val authenticated: Boolean
        get() = user != null

    /**
     * Session user data
     */
    val user: UserData?
        get() {
            return sPref.run {
                val email = getString("email", null)
                val display_name = getString("display_name", null)
                if (email != null && display_name != null) UserData(email, display_name)
                null
            }
        }

    /**
     * Set session user data
     */
    fun setUserData(userData: UserData) {
        sPref.edit().run {
            putString("email", userData.email)
            putString("display_name", userData.display_name)
            apply()
        }
    }
}