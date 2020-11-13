package solutions.cvs.videoroom.base

import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import solutions.cvs.videoroom.R


/**
 * Activity base class
 */
open class BaseActivity : AppCompatActivity() {

    protected fun setStartDestination(destinationId: Int) {
        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment?
        val navController = navHost!!.navController
        val navInflater = navController.navInflater
        val graph = navInflater.inflate(R.navigation.nav_graph)
        graph.startDestination = destinationId
        navController.setGraph(graph);
    }
}