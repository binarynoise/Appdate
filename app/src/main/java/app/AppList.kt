package de.binarynoise.appdate

import androidx.recyclerview.widget.SortedList
import androidx.recyclerview.widget.SortedListAdapterCallback
import de.binarynoise.appdate.Log.Level.Warn
import de.binarynoise.appdate.Log.Place.Logcat
import de.binarynoise.appdate.Log.Place.Toast
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Main
import java.io.IOException

object AppList {
	
	private const val SHARED_PREFS_APP_LIST_LABEL = "appList"
	internal val all: Array<App>
		get() = synchronized(appList) {
			return Array(appList.size()) { appList.get(it) }
		}
	private val appList: SortedList<App> = SortedAppList()
	@Volatile
	private var loaded = false
	private val log = Log(isDebug = true)
	val size: Int
		get() = appList.size()
	
	fun addToList(app: App) {
		appList.add(app)
	}
	
	@RunInBackground
	suspend fun checkForUpdates() {
		AppOverviewFragment.setRefreshing(true)
		
		load()
		Notifier.initNotificationGroups()
		
		val availableAppTemplates = AppTemplate.getAvailableAppTemplates()
		withTimeoutOrNull(60_000) {
			coroutineScope {
				availableAppTemplates.values.forEach { at ->
					launch {
						try {
							addToList(at)
						} catch (e: IOException) {
							log.log(getString(R.string.fetching_templates_failed), e, Warn, Toast, Logcat)
						}
					}
				}
			}
		} ?: log.log(getString(R.string.fetching_templates_timed_out), Warn, Toast, Logcat)
		
		
		coroutineScope {
			all.forEach { app ->
				launch {
					withTimeoutOrNull(60_000) {
						try {
							app.checkForUpdates()
						} catch (e: IOException) {
							log.log(getString(R.string.fetching_updates_failed_s, app.installedName), e, Warn, *Log.Place.all)
						}
					} ?: log.log(getString(R.string.fetching_updates_timed_out), Warn, *Log.Place.all)
				}
			}
		}
		
		withContext(Main) {
			appList.endBatchedUpdates()
		}
		AppOverviewFragment.setRefreshing(false)
	}
	
	fun findById(id: Int): App? {
		appList.forEach { app ->
			if (app.getId() == id) return app
		}
		return null
	}
	
	fun findByPackageName(packageName: String?): App? {
		if (packageName == null) return null
		
		for (i in 0 until appList.size()) {
			val app = appList.get(i)
			if (app.packageName == packageName) return app
		}
		return null
	}
	
	fun findByUrl(url: String?): App? {
		if (url == null) return null
		
		appList.forEach { app ->
			if (app.updateUrl == url) return app
		}
		
		return null
	}
	
	fun forPosition(position: Int): App = appList.get(position)
	
	fun load() {
		if (loaded) return
		loaded = true
		synchronized(appList) {
			if (appList.size() != 0) return
			
			val list: Array<App>? = fromJson(sharedPreferences.getString(SHARED_PREFS_APP_LIST_LABEL, "[]"))
			runBlocking {
				withContext(Main) {
					appList.addAll(list?.asList() ?: emptyList())
				}
			}
			
			save()
		}
	}
	
	internal fun removeFromList(app: App) = appList.remove(app)
	
	fun save() {
		val size = appList.size()
		if (size == 0) return
		val list = List(size) { index ->
			appList.get(index)
		}
		
		val json = list.toJson()
		
		val local = mapper.readTree(json)
		val remote = mapper.readTree(sharedPreferences.getString(SHARED_PREFS_APP_LIST_LABEL, null) ?: "")
		
		if (local != remote) {
			sharedPreferences.edit().putString(SHARED_PREFS_APP_LIST_LABEL, json).apply()
		}
		
		upload()
	}
	
	@RunInBackground
	@Throws(IOException::class)
	private suspend fun addToList(appTemplate: AppTemplate) {
		val app = App(appTemplate)
		withContext(Main) {
			appList.add(app)
		}
	}
	
	private fun upload() = GlobalScope.launch {
		try {
			AppTemplate.updateAppTemplates()
		} catch (e: IOException) {
			log.log(getString(R.string.could_not_upload_templates), e, Warn, Logcat)
		}
	}
	
	class SortedAppListAdapterCallback : SortedListAdapterCallback<App>(AppOverviewFragment.adapter) {
		override fun compare(first: App?, second: App?): Int {
			return when {
				first == second -> 0
				second == null -> 1
				first == null -> -1
				else -> first.installedName.compareTo(second.installedName, ignoreCase = true)
			}
		}
		
		override fun areContentsTheSame(oldItem: App, newItem: App): Boolean {
			return (oldItem.installedName != newItem.installedName) && oldItem.installedVersion == newItem.installedVersion
		}
		
		override fun areItemsTheSame(item1: App, item2: App): Boolean {
			return item1.updateUrl == item2.updateUrl
		}
	}
	
	class SortedAppList : SortedList<App>(App::class.java, SortedAppListAdapterCallback()) {
		override fun add(item: App): Int {
			log.log("adding app ${item.installedName}")
			val add = super.add(item)
			if (loaded) save()
			return add
		}
		
		override fun updateItemAt(index: Int, item: App) {
			super.updateItemAt(index, item)
			if (loaded) save()
		}
	}
}

inline fun <T : Any?> SortedList<T>.forEach(action: (T) -> Unit) {
	for (i in 0 until size()) {
		action(get(i))
	}
}
