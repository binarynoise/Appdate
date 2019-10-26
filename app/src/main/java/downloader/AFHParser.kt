package de.binarynoise.appdate

import android.annotation.SuppressLint
import android.net.http.SslError
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.webkit.*
import androidx.annotation.RequiresApi
import com.vdurmont.semver4j.Semver
import de.binarynoise.appdate.WebsiteParser.getVersionFromFileNameOrPath
import de.binarynoise.appdate.WebsiteParser.lastCheckedForUpdates
import de.binarynoise.appdate.WebsiteParser.lastDownloadUrls
import de.binarynoise.appdate.WebsiteParser.lastVersions
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Default
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeoutException

object AFHParser {
	
	private val log = Log(isDebug = false)
	private val api: URL
	private val cookieManager = CookieManager.getInstance()
	private const val fidParam = "/?fid="
	
	private const val apiURL = "https://androidfilehost.com/libs/otf/mirrors.otf.php"
	
	init {
		try {
			api = URL(apiURL)
		} catch (e: MalformedURLException) {
			throw RuntimeException(e)
		}
	}
	
	private const val getHtmlJs = "(function() { return document.body.innerHTML; })();"
	
	@SuppressLint("SetJavaScriptEnabled")
	@Throws(IOException::class)
	fun parse(app: App): Tupel<Semver, String> {
		log.log("afh")
		val updateUrl = app.updateUrl
		
		val waiter = Waiter()
		var exception: IOException? = null
		waiter.timeout = 60_000
		val handler = Handler(Looper.getMainLooper())
		
		var webView: WebView? = null
		try {
			var fid: Long = -1
			
			val file = File(globalContext.cacheDir.toString() + "/WebView/Crashpad")
			if (!file.exists()) file.mkdirs()
			handler.post {
				webView = WebView(globalContext)
				log.log("webview constructed")
				
				val setting = webView!!.settings
				setting.javaScriptEnabled = true // this is needed to pass the afh browser check
				setting.allowFileAccess = false // protect ourselves
				// save data, as we don't display anything, we don't need to download all the images
				setting.loadsImagesAutomatically = false
				
				webView!!.webViewClient = object : WebViewClient() {
					var triedAgain = false
					
					override fun onPageFinished(view: WebView, url: String) {
						super.onPageFinished(view, url)
						log.log("done loading $url")
						
						view.evaluateJavascript(getHtmlJs) { htmlEscaped ->
							log.log("done getting html")
							GlobalScope.launch(Default){
								val html: String? = fromJson(htmlEscaped)
								val split = html?.split("\"")
								split?.find { it.startsWith(fidParam) }?.run {
									fid = substring(fidParam.length).toLong()
								}
								if (fid == -1L && !triedAgain) {
									triedAgain = true
									handler.post {
										webView!!.loadUrl(updateUrl)
									}
								} else waiter.wake()
							}
						}
					}
					
					@RequiresApi(Build.VERSION_CODES.M)
					override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
						super.onReceivedError(view, request, error)
						exception = IOException(error.description.toString())
						waiter.wake()
					}
					
					@Suppress("DEPRECATION")
					override fun onReceivedError(view: WebView, errorCode: Int, description: String, failingUrl: String) {
						super.onReceivedError(view, errorCode, description, failingUrl)
						exception = IOException(description)
					}
					
					override fun onReceivedHttpError(
						view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse
					) {
						super.onReceivedHttpError(view, request, errorResponse)
						exception = IOException(errorResponse.reasonPhrase)
						waiter.wake()
					}
					
					override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
						super.onReceivedSslError(view, handler, error)
						handler.cancel()
						exception = IOException(error.toString())
						waiter.wake()
					}
				}
				
				webView!!.loadUrl(updateUrl)
			}
			
			try {
				waiter.sleep() // wait for webview to be constructed and site to load
			} catch (e: TimeoutException) {
				throw IOException(
					getString(R.string.waiting_for_timed_out_sd,
						"website $updateUrl to load", waiter.timeout
					), e
				)
			}
			
			exception?.throwIt()
			
			if (fid == -1L) {
//				log.logLines(html!!, Warn)
				val cookie = cookieManager.getCookie("https://androidfilehost.com/")
				log.logSplit(cookie, ";", "cookies")
				throw IOException("${getString(R.string.no_releases_found)}: $updateUrl")
			}
			
			log.log("fid=$fid")
			
			var afhResponse: AFHResponse = fetchMirrors(fid)
			
			if (afhResponse.MIRRORS.isEmpty()) {
				afhResponse = fetchMirrors(fid) // try again
				if (afhResponse.MIRRORS.isEmpty()) {
					throw IOException(getString(R.string.no_releases_found_for_app_under_fid_sd, app.installedName, fid))
				}
			}
			
			val downloadUrl = afhResponse.MIRRORS[0].url
			val version = getVersionFromFileNameOrPath(downloadUrl) ?: throw IOException(getString(R.string.no_version_in_s, updateUrl))
			lastVersions[updateUrl] = version
			lastDownloadUrls[updateUrl] = downloadUrl
			lastCheckedForUpdates[updateUrl] = System.currentTimeMillis()
			
			return Tupel(version, downloadUrl)
		} finally {
			handler.post {
				webView?.destroy()
			}
			log.log("afh done")
		}
	}
	
	private fun fetchMirrors(fid: Long): AFHResponse {
		var json: String? = null
		
		with(api.openConnection() as HttpURLConnection) {
			requestMethod = "POST"
			setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
			val cookie = cookieManager.getCookie("https://androidfilehost.com/")
			log.logSplit("cookie", cookie, "end cookie", ";")
			setRequestProperty("Cookie", cookie) // to pass security check
			setRequestProperty("Referer", "https://androidfilehost.com/?fid=$fid")
			setRequestProperty("X-Requested-With", "XMLHttpRequest")
			setRequestProperty("X-MOD-SBB-CTYPE", "xhr")
			doOutput = true
			useCaches = false
			val s = "submit=submit&action=getdownloadmirrors&fid=$fid"
			log.log(s)
			outputStream.use { it.write(s.toByteArray(StandardCharsets.UTF_8)) }
			inputStream.use { input ->
				ByteArrayOutputStream().use { output ->
					input.copyTo(output)
					json = output.toString()
				}
			}
		}
		val afhResponse: AFHResponse?
		
		try {
			afhResponse = fromJson(json?.split("\n")?.last()) ?: throw KotlinNullPointerException()
		} catch (e: Throwable) {
			json?.lines()?.forEach {
				log.log("json: $it", Log.Level.Error, Log.Place.Logcat)
			}
			throw IOException(getString(R.string.json_parsing_failed), e)
		}
		
		if ("200" != afhResponse.CODE) throw IOException(
			getString(R.string.afh_returned_code_s_with_message, afhResponse.CODE) + "\n" + afhResponse.MESSAGE
		)
		return afhResponse
	}
}

