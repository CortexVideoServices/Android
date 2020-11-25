package solutions.cvs.videoroom


import android.Manifest
import android.content.Context
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.navigation.fragment.NavHostFragment
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions

const val RC_APPLICATION_PERMISSION = 1234

/**
 * Application main activity
 */
class MainActivity : AppCompatActivity() {

    var permissionsGranted: Boolean = false
        private set

    val userSession: UserSession by viewModels()
    val conferenceVM: ConferenceVM by viewModels()

    protected fun setStartDestination(destinationId: Int) {
        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment?
        val navController = navHost!!.navController
        val navInflater = navController.navInflater
        val graph = navInflater.inflate(R.navigation.nav_graph)
        graph.startDestination = destinationId
        navController.setGraph(graph);
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        val sPref = application.applicationContext.getSharedPreferences("videoroom", Context.MODE_PRIVATE)
        sPref.edit().run {
            putString("baseUrl", "https://cvs.solutions")
            apply()
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        userSession.authenticated.observe(this, Observer {authenticated ->
            if (authenticated) setStartDestination(R.id.destination_Loader)
            else setStartDestination(R.id.destination_Login)
        })
        requestPermissions()
    }

    @AfterPermissionGranted(RC_APPLICATION_PERMISSION)
    fun requestPermissions() {
        val perms = arrayOf(
            Manifest.permission.INTERNET,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.MODIFY_AUDIO_SETTINGS
        )
        if (EasyPermissions.hasPermissions(applicationContext, *perms))
            onPermissionsGranted()
        else {
            EasyPermissions.requestPermissions(
                this,
                "This app needs access to your camera and mic to make video calls",
                RC_APPLICATION_PERMISSION,
                *perms
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    private fun onPermissionsGranted() {
        permissionsGranted = true
    }


}