package de.binarynoise.appdate

import kotlinx.coroutines.*
import net.erdfelt.android.apk.AndroidApk
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL

object Downloader {
	@Throws(IOException::class)
	fun download(app: App, progressCallback: (Int, Int) -> Unit) = runBlocking(Dispatchers.IO) {
		val fileName = app.installedName + "-v" + app.updateVersion + ".apk"
		val externalCacheDir = globalContext.externalCacheDir!!
		externalCacheDir
			.list()!!
			.filter { it.startsWith(app.installedName) || it.startsWith("unknown", ignoreCase = true) }
			.forEach { File(it).delete() }
		
		val targetFile = externalCacheDir.resolve(fileName).absolutePath
		val cacheFile = CacheFile(targetFile)
		app.cacheFile = cacheFile
		
		if (!hasInternetConnection()) throw IOException(getString(R.string.no_network_found_try_later))
		
		val src = URL(app.downloadURLString)
		val urlConnection = src.openConnection()
		
		urlConnection.getInputStream().buffered().use { input ->
			FileOutputStream(targetFile, false).buffered().use { output ->
				val size = urlConnection.contentLength
				cacheFile.fileSize = size
				var progress = 0
				val buffer = ByteArray(0x10000) // 64kB
				
				do {
					val count: Int = input.read(buffer)
					if (count >= 0) output.write(buffer, 0, count)
					progress += count
					progressCallback(progress, size)
				} while (count >= 0)
			}
		}
		app.packageName = AndroidApk(File(targetFile)).packageName
	}
}
