package de.binarynoise.appdate

import com.vdurmont.semver4j.Semver
import de.binarynoise.appdate.WebsiteParser.getVersionFromFileNameOrPath
import de.binarynoise.appdate.WebsiteParser.lastCheckedForUpdates
import de.binarynoise.appdate.WebsiteParser.lastDownloadUrls
import de.binarynoise.appdate.WebsiteParser.lastVersions
import de.binarynoise.appdate.WebsiteParser.toAbsolutePath
import java.io.IOException
import java.net.URL

object GenericParser {
	@Throws(IOException::class)
	fun parse(app: App): Tupel<Semver, String> {
		val updateUrl = app.updateUrl
		
		URL(updateUrl).openStream().bufferedReader().use { bufferedReader ->
			var line = bufferedReader.readLine()
			while (line != null) {
				if (line.contains(".apk") && line.contains("href")) {
					line.split('"').filter { it.contains(".apk") }.forEach { path ->
						val version = getVersionFromFileNameOrPath(path)
						if (version != null) {
							val downloadUrl = toAbsolutePath(updateUrl, path)
							lastVersions[updateUrl] = version
							lastDownloadUrls[updateUrl] = downloadUrl
							lastCheckedForUpdates[updateUrl] = System.currentTimeMillis()
							return Tupel(version, downloadUrl)
						}
					}
				}
				line = bufferedReader.readLine()
			}
		}
		throw IOException(getString(R.string.no_releases_found_for_app_s, app.installedName))
	}
}
