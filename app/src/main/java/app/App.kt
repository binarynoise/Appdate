package de.binarynoise.appdate

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.navigation.NavDeepLinkBuilder
import com.fasterxml.jackson.annotation.JsonIgnore
import com.vdurmont.semver4j.Semver
import com.vdurmont.semver4j.Semver.SemverType.LOOSE
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

class App {
	
	var cacheFile: CacheFile? = null
		set(value) {
			val changed = field != value
			if (changed) {
				field?.delete()
			}
			field = value
			if (changed) {
				notifyUiListeners()
			}
		}
	@JsonIgnore
	var downloadURLString: String? = null
		set(value) {
			val changed = field != value
			field = value
			if (changed) notifyUiListeners()
		}
	@JsonIgnore
	var downloading: Boolean = false
		set(value) {
			val changed = field != value
			field = value
			if (changed) notifyUiListeners()
		}
	val hasUpdates: Boolean
		get() {
			val haveUpdate = installedVersion.isLowerThan(updateVersion?.withClearedSuffixAndBuild() ?: Semver("0.0.0"))
			if (!haveUpdate) cacheFile = null
			return haveUpdate
		}
	var installedName: String = "unknown"
		set(value) {
			val changed = field != value
			field = value
			if (changed) notifyUiListeners()
		}
	val installedVersion: Semver
		get() {
			try {
				@Suppress("DEPRECATION") return Semver(
					if (useVersionNumberInsteadOfName) packageInfo.versionCode.toString() else packageInfo.versionName, LOOSE
				)
			} catch (e: PackageManager.NameNotFoundException) {
				return Semver("0.0.0")
			}
		}
	@JsonIgnore
	var installing: Boolean = false
		set(value) {
			val changed = field != value
			field = value
			if (changed) notifyUiListeners()
		}
	private val lock = ReentrantLock()
	var packageName: String? = null
		set(value) {
			val changed = field != value
			field = value
			if (changed) notifyUiListeners()
		}
	val updateUrl: String
	var updateVersion: Semver? = null
		set(value) {
			val changed = field != value
			field = value
			if (changed) notifyUiListeners()
			updateNotification()
		}
	var updating: Boolean = false
		set(value) {
			val changed = field != value
			field = value
			if (changed) notifyUiListeners()
		}
	var useVersionNumberInsteadOfName: Boolean = false
	
	@JsonIgnore
	private var changeListenerList: MutableMap<Lifecycle, () -> Unit> = ConcurrentHashMap(2)
	
	fun addChangeListener(lifecycle: Lifecycle, listener: () -> Unit) {
		val observer = object : LifecycleObserver {
			@OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
			fun onDetatch() {
				changeListenerList.remove(lifecycle)
			}
		}
		lifecycle.addObserver(observer)
		changeListenerList[lifecycle] = listener
	}
	
	@JsonIgnore
	private var downloadProgressListenerList: MutableMap<Lifecycle, (Int, Int) -> Unit> = ConcurrentHashMap(2)
	
	fun addDownloadProgressListener(lifecycle: Lifecycle, listener: (Int, Int) -> Unit) {
		val observer = object : LifecycleObserver {
			@OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
			fun onDetatch() {
				changeListenerList.remove(lifecycle)
			}
		}
		lifecycle.addObserver(observer)
		downloadProgressListenerList[lifecycle] = listener
	}
	
	@Throws(IOException::class)
	fun checkForUpdates(): Boolean {
		WebsiteParser.getLatestVersion(this)
		return hasUpdates
	}
	
	/**
	 * @return if app has been removed from list and fragment needs to be closed
	 */
	fun delete(): Pair<Boolean, Boolean> {
		var navUp = false
		var fileDeleted = true
		lock.withTryLock {
			cacheFile?.delete()
			fileDeleted = cacheFile?.file?.exists() != true // deleted or cacheFile non-existent
			if (fileDeleted) {
				cacheFile = null
				if (!isInstalled()) {
					AppList.removeFromList(this)
					navUp = true
				}
			}
		}
		return Pair(navUp, fileDeleted)
	}
	
	@Throws(IOException::class)
	fun download() {
		if (downloading) return
		downloading = true
		try {
			Downloader.download(this) { progress, max ->
				downloadProgressListenerList.forEach { (_, cb) -> cb(progress, max) }
			}
		} finally {
			downloading = false
			if (!cacheFile.isValid())
				cacheFile = null
		}
	}
	
	private val packageManager
		@JsonIgnore
		get() = globalContext.packageManager
	
	private val packageInfo
		@JsonIgnore
		get() = packageManager.getPackageInfo(packageName ?: "", 0)
	
	@JsonIgnore
	fun getIcon(): Drawable = try {
		packageInfo.applicationInfo.loadIcon(packageManager)
	} catch (e: PackageManager.NameNotFoundException) {
		ColorDrawable(Color.TRANSPARENT)
	}
	
	fun getId(): Int = updateUrl.hashCode()
	
	@Throws(IOException::class)
	fun install() {
		if (installing) return
		installing = true
		try {
			Installer.install(this)
			Notifier.clearNotification(getId())
		} finally {
			installing = false
		}
	}
	
	private fun updateNotification() {
		val id = getId()
		if (hasUpdates) {
			
			val title = getString(R.string.update_for_s, installedName)
			
			val text: String = if (isInstalled()) getString(R.string.found_update_sss,
				installedName, installedVersion, updateVersion
			)
			else getString(R.string.can_install_app_ss, installedName, updateVersion)
			
			val drawable = getIcon()
			
			val action =
				NavDeepLinkBuilder(globalContext).setGraph(R.navigation.nav_graph).setDestination(R.id.nav_appDetail)
					.setArguments(AppDetailFragmentArgs(id).toBundle()).createPendingIntent()
			
			Notifier.notification(id, Notifier.Groups.UPDATE, drawable, title, text, action)
		} else Notifier.clearNotification(id)
	}
	
	fun isInstalled(): Boolean {
		if (packageName == null || installedName.isEmpty()) return false
		return try {
			installedName = packageManager.getApplicationLabel(packageInfo.applicationInfo).toString()
			true
		} catch (e: PackageManager.NameNotFoundException) {
			false
		}
	}
	
	private fun notifyUiListeners() {
		changeListenerList.forEach { (_, cb) -> cb() }
		AppList.save()
	}
	
	@RunInBackground
	@Throws(IOException::class)
	constructor(installedName: String, updateUrl: String) {
		this.installedName = installedName.trim { it == ' ' }
		this.updateUrl = updateUrl
		
		checkForUpdates()
	}
	
	@RunInBackground
	@Throws(IOException::class)
	constructor(packageInfo: PackageInfo, pm: PackageManager, updateUrl: String) {
		installedName = packageInfo.applicationInfo.loadLabel(pm).toString()
		packageName = packageInfo.packageName
		this.updateUrl = updateUrl
		
		checkForUpdates()
	}
	
	@RunInBackground
	@Throws(IOException::class)
	constructor(template: AppTemplate) {
		installedName = template.name
		packageName = template.packageName
		updateUrl = template.updateUrl
		isInstalled()
		checkForUpdates()
	}
	
	/**
	 * private no-args constructor for correct json deserialization
	 */
	private constructor() {
		updateUrl = ""
	}
}

fun <T> Lock.withTryLock(action: () -> T): Boolean {
	if (tryLock()) try {
		action()
	} finally {
		unlock()
		return true
	}
	return false
}
