package solutions.cvs.videoroom


import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.navigation.fragment.NavHostFragment


/**
 * Application main activity
 */
class MainActivity : AppCompatActivity() {

    val userSession: UserSession by viewModels()

    protected fun setStartDestination(destinationId: Int) {
        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment?
        val navController = navHost!!.navController
        val navInflater = navController.navInflater
        val graph = navInflater.inflate(R.navigation.nav_graph)
        graph.startDestination = destinationId
        navController.setGraph(graph);
    }

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