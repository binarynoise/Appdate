package de.binarynoise.appdate

import android.content.pm.PackageManager

import de.binarynoise.appdate.Log.Level.Warn
import de.binarynoise.appdate.Log.Place.Logcat
import de.binarynoise.appdate.Log.Place.Toast
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO

import java.io.IOException
import java.util.*

class AppTemplate : Comparable<AppTemplate> {
	
	val name: String
	val packageName: String
	val updateUrl: String
	override fun compareTo(other: AppTemplate): Int {
		return if (other == this) 0
		else packageName.compareTo(other.packageName)
	}
	
	override fun equals(other: Any?): Boolean = (other !is AppTemplate) && compareTo((other as AppTemplate?)!!) == 0
	override fun toString(): String = "($packageName, $name, $updateUrl)"
	private fun asEntryRow(): List<Any> = listOf(packageName, name, updateUrl)
	private fun isInstalled(): Boolean {
		if (packageName.isEmpty()) return false
		val packageManager = globalContext.packageManager
		return try {
			packageManager.getPackageInfo(packageName, 0)
			true
		} catch (e: PackageManager.NameNotFoundException) {
			false
		}
	}
	
	internal constructor(app: App) {
		packageName = app.packageName!!
		updateUrl = app.updateUrl
		name = app.installedName
	}
	
	private constructor(packageName: String, name: String, updateUrlString: String) {
		this.packageName = packageName
		this.name = name
		updateUrl = updateUrlString
	}
	
	companion object {
		
		private val appTemplates: MutableMap<String, AppTemplate> =
			Collections.synchronizedMap(HashMap<String, AppTemplate>())
		private val appTemplatesOnServer: MutableMap<String, AppTemplate> =
			Collections.synchronizedMap(HashMap<String, AppTemplate>())
		
		fun getAvailableAppTemplates(): Map<String, AppTemplate> {
			donwloadList()
			
			val available = HashMap<String, AppTemplate>()
			
			synchronized(appTemplates) {
				for ((_, at: AppTemplate) in appTemplates) {
					val installed = at.isInstalled()
					val app = AppList.findByUrl(at.updateUrl) ?: AppList.findByPackageName(at.packageName)
					
					if (installed && app == null) available[at.packageName] = at
				}
			}
			
			return Collections.unmodifiableMap(available)
		}
		
		@Volatile
		private var lastFetched: Long = -1
		private val log = Log()
		
		@RunInBackground
		@Throws(IOException::class)
		suspend fun updateAppTemplates() = withContext(IO) {
			val apps = AppList.all
			
			for (app in apps) if (app.packageName != null && app.isInstalled()) {
				val appTemplate = AppTemplate(app)
				appTemplates[appTemplate.updateUrl] = appTemplate
			}
			
			uploadList()
		}
		
		@RunInBackground
		@Throws(IOException::class)
		private fun donwloadList() {
			if (!hasInternetConnection()) throw IOException(getString(R.string.no_network_found_try_later))
			synchronized(appTemplates) {
				if (System.currentTimeMillis() - lastFetched > 5000) {
					val values = GoogleSheetsBridge.getValues()
					appTemplatesOnServer.clear()
					
					for (row in values) {
						val appTemplate = fromEntryRow(row)
						if (appTemplate != null) appTemplatesOnServer[appTemplate.packageName] = appTemplate
					}
					lastFetched = System.currentTimeMillis()
				}
				appTemplates.clear()
				appTemplates.putAll(appTemplatesOnServer)
			}
		}
		
		private fun fromEntryRow(row: List<Any>): AppTemplate? {
			return try {
				val packageName = row[0] as String
				val name = row[1] as String
				val url = row[2] as String
				
				AppTemplate(packageName, name, url)
			} catch (e: IndexOutOfBoundsException) {
				log.log("Could not create App from Template", e, Warn, Logcat, Toast)
				null
			} catch (e: ClassCastException) {
				log.log("Could not create App from Template", e, Warn, Logcat, Toast)
				null
			}
		}
		
		@Suppress("UNREACHABLE_CODE")
		@RunInBackground
		@Throws(IOException::class)
		private fun uploadList() {
			if (appTemplatesOnServer.isEmpty() && appTemplates.isEmpty() || appTemplatesOnServer.keys == appTemplates.keys) return
			
			return
			
			//TODO
			val values = ArrayList<List<Any>>()
			
			for (appTemplate in appTemplates.values) values.add(appTemplate.asEntryRow())
			
			values.sortWith(Comparator { o1, o2 ->
				val s1 = o1[0] as String
				val s2 = o2[0] as String
				s1.compareTo(s2)
			})
			
			synchronized(appTemplatesOnServer) {
				GoogleSheetsBridge.updateValues(values)
				appTemplatesOnServer.clear()
				appTemplatesOnServer.putAll(appTemplates)
			}
			lastFetched = System.currentTimeMillis()
		}
	}
}
