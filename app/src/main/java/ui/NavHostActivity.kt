package de.binarynoise.appdate

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.onNavDestinationSelected
import kotlinx.android.synthetic.main.nav_host_activity.*

class NavHostActivity : AppCompatActivity() {
	
	private lateinit var appBarConfiguration: AppBarConfiguration
	private lateinit var navController: NavController
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.nav_host_activity)
		
		setSupportActionBar(toolbar)
		
		navController = findNavController(R.id.nav_host_fragment)
		appBarConfiguration = AppBarConfiguration.Builder(navController.graph).build()
		NavigationUI.setupWithNavController(toolbar, navController)
		
		navController.addOnDestinationChangedListener { _, _, _ ->
			invalidateOptionsMenu()
		}
	}
	
	override fun onBackPressed() {
		if (navController.currentDestination?.id ?: -1 == R.id.nav_appOverview) super.onBackPressed()
		else navController.navigateUp()
	}
	
	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return item.onNavDestinationSelected(navController) || super.onOptionsItemSelected(item)
	}
	
	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		val id = navController.currentDestination?.id ?: 0
		if (id == R.id.nav_appOverview || id == R.id.nav_appDetail || id == R.id.nav_addApp)
			menuInflater.inflate(R.menu.main, menu)
		return super.onCreateOptionsMenu(menu)
	}
	
	companion object {
		val log: Log = Log(isDebug = false)
	}
}
