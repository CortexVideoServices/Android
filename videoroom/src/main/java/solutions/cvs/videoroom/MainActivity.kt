package solutions.cvs.videoroom


import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import solutions.cvs.videoroom.base.BaseActivity
import solutions.cvs.videoroom.user.UserSession

/**
 * Application main activity
 */
class MainActivity : BaseActivity() {

    val userSession: UserSession by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        userSession.authenticated.observe(this, Observer {authenticated ->
            if (authenticated) setStartDestination(R.id.destination_CreateConference)
            else setStartDestination(R.id.destination_Login)
        })
    }

}