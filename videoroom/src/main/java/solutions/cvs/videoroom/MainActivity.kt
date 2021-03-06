package solutions.cvs.videoroom


import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.*
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.NavHostFragment
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions
import solutions.cvs.videoroom.ui.SetUrlDialog


const val RC_APPLICATION_PERMISSION = 1234
const val RESTART_INTENT_ID = 321

/**
 * Application main activity
 */
class MainActivity : AppCompatActivity() {

    var permissionsGranted: Boolean = false
        private set

    val userSession: UserSession by viewModels()
    val conferenceVM: ConferenceVM by viewModels()

    lateinit var dlgSetUrl: SetUrlDialog;

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
        if (sPref.getString("baseUrl", null) == null)
            sPref.edit().run {
                putString("baseUrl", "https://cvs.solutions")
                apply()
            }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        dlgSetUrl = SetUrlDialog()

        userSession.authenticated.observe(this, Observer { authenticated ->
            if (authenticated) setStartDestination(R.id.destination_Loader)
            else setStartDestination(R.id.destination_Login)
        })
        requestPermissions()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                dlgSetUrl.show(supportFragmentManager, "SetUrl")
                true
            }
            R.id.action_paste_invitation -> {
                val sessionId = pasteSessionId();
                if (sessionId != null) startSession(sessionId)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
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

    private fun pasteSessionId(): String? {
        var result: String? = null
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        if (clipboardManager.hasPrimaryClip() &&
            clipboardManager.primaryClipDescription?.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)!!
        ) {
            val text = clipboardManager.primaryClip?.getItemAt(0)?.text
            if (text != null) {
                val p = text.findLastAnyOf(listOf("/#/conference/"))
                if (p != null) {
                    val offset = p.first + 14;
                    result = text.substring(offset)
                    val data = ClipData.newPlainText("", "")
                    clipboardManager.setPrimaryClip(data)
                }
            }
        }
        return result
    }

    private fun startSession(sessionId: String) {
        val bundle = bundleOf("sessionId" to sessionId)
        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment?
        val navController = navHost!!.navController
        navController.navigate(R.id.action_3VideoRoom, bundle);
    }


    fun triggerRestart(context: Context) {
        val startActivity = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            RESTART_INTENT_ID,
            startActivity,
            PendingIntent.FLAG_CANCEL_CURRENT
        )
        Handler(Looper.getMainLooper()).postDelayed({
            val mgr = context.getSystemService(ALARM_SERVICE) as AlarmManager
            mgr[AlarmManager.RTC, System.currentTimeMillis() + 100] = pendingIntent
            System.exit(0)
        }, 500)
    }
}