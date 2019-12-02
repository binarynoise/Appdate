package de.binarynoise.appdate

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.navigation.NavDeepLinkBuilder
import com.fasterxml.jackson.annotation.JsonIgnore
import com.vdurmont.semver4j.Semver
import com.vdurmont.semver4j.Semver.SemverType.LOOSE
import de.binarynoise.appdate.Preferences.installAfterDownload
import kotlinx.coroutines.*
import java.io.IOException
import java.util.*
import java.util.Collections.newSetFromMap
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
		get() = try {
			@Suppress("DEPRECATION")
			val version = if (useVersionNumberInsteadOfName) packageInfo.versionCode.toString()
			else packageInfo.versionName
			Semver(version, LOOSE)
		} catch (e: PackageManager.NameNotFoundException) {
			Semver("0", LOOSE)
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
	private var changeListeners: MutableSet<() -> Unit> = WeakSet()
	fun addChangeListener(listener: () -> Unit) {
		changeListeners.add(listener)
	}
	
	@JsonIgnore
	private var downloadProgressListeners: MutableSet<(Int, Int) -> Unit> = WeakSet()
	fun addDownloadProgressListener(listener: (Int, Int) -> Unit) {
		downloadProgressListeners.add(listener)
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
				downloadProgressListeners.forEach { cb -> cb(progress, max) }
			}
			if(cacheFile.isValid() && Preferences[installAfterDownload, false])
				GlobalScope.launch { install() }
		} finally {
			downloading = false
			if (!cacheFile.isValid()) cacheFile = null
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
			
			val text: String =
				if (isInstalled()) getString(R.string.found_update_sss, installedName, installedVersion, updateVersion)
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
		changeListeners.forEach { cb -> cb() }
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

object WeakSet {
	operator fun <K> invoke(initalCapacity: Int = 2): MutableSet<K> = newSetFromMap(WeakHashMap<K, Boolean>(initalCapacity))
}
