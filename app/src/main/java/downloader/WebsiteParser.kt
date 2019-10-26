package de.binarynoise.appdate

import com.vdurmont.semver4j.Semver
import com.vdurmont.semver4j.Semver.SemverType.LOOSE
import com.vdurmont.semver4j.SemverException
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

object WebsiteParser {
	//Map updateUrls to when last checked
	internal val lastCheckedForUpdates: MutableMap<String, Long> = ConcurrentHashMap()
	internal val lastVersions: MutableMap<String, Semver> = ConcurrentHashMap()
	internal val lastDownloadUrls: MutableMap<String, String> = ConcurrentHashMap()
	private val log = Log(isDebug = false)
	
	@Throws(IOException::class)
	fun getLatestVersion(app: App) {
		val host = URL(app.updateUrl).host
		val updateUrl = app.updateUrl
		
		log.log("updateUrl=${app.updateUrl}")
		
		val lastChecked = lastCheckedForUpdates[updateUrl]
		val lastVersion = lastVersions[updateUrl]
		val lastDownloadUrl = lastDownloadUrls[updateUrl]
		if (lastChecked != null && System.currentTimeMillis() - lastChecked < 10000 && lastVersion != null && lastDownloadUrl != null) {
			log.log("serving cached entry")
			app.updateVersion = lastVersion
			app.downloadURLString = lastDownloadUrl
		}
		
		log.log("host: $host")
		
		if (!hasInternetConnection()) throw IOException(getString(R.string.no_network_found_try_later))
		
		val t = when {
			"androidfilehost.com".equals(host, ignoreCase = true) -> AFHParser.parse(app)
			"repo.xposed.info".equals(host, ignoreCase = true) -> XposedParser.parse(app)
			else -> GenericParser.parse(app)
		}
		
		app.updateVersion = t._1
		app.downloadURLString = t._2
	}
	
	@Throws(SemverException::class)
	fun getVersionFromFileNameOrPath(path: String): Semver? {
		val path1 = path.split("?")[0]
		val splits = path1.split("/")
		for (i in splits.indices.reversed()) {
			val split = splits[i]
			if (split.matches("^\\D*\\d+[-_.\\d\\w]*".toRegex())) {
				val fileName = split.replace("^\\D*".toRegex(), "").replace("_", "-").replace(".apk$".toRegex(), "")
				try {
					return Semver(fileName, LOOSE)
				} catch (e: Exception) {
					log.log(e.localizedMessage, e, Log.Level.Warn, Log.Place.Logcat)
				}
			}
		}
		return null
	}
	
	@Throws(MalformedURLException::class)
	fun toAbsolutePath(base: String, rel: String): String {
		return URL(URL(base), rel).toString()
	}
}
