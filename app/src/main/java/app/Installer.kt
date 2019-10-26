package de.binarynoise.appdate

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.pm.PackageInstaller.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.core.os.bundleOf
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Main
import net.erdfelt.android.apk.AndroidApk
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

private val waiter: MutableMap<Int, Waiter?> = ConcurrentHashMap()
private val result: MutableMap<Int, Bundle?> = ConcurrentHashMap()

private val log = Log(isDebug = true)

object Installer {
	
	@Throws(IOException::class, InstallNotPermittedException::class)
	fun install(app: App) {
		if (app.cacheFile == null) throw IOException(getString(R.string.s_is_null, getString(R.string.cache_file)))
		
		val apkfile = app.cacheFile!!.file
		if (Shell.rootAccess()) { // TODO allow to select
			installRoot(apkfile)
		} else {
			installNonRoot(apkfile, app.getId())
		}
	}
	
	@Throws(IOException::class)
	private fun installNonRoot(apkfile: File, id: Int) {
		val context = globalContext
		val packageManager = context.packageManager
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !packageManager.canRequestPackageInstalls()) {
			val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
			intent.data = Uri.parse("package:${BuildConfig.APPLICATION_ID}")
			InstallRequestActivity.startRequestInstallPermissionDialog(id)
			val w = Waiter()
			waiter[id] = w
			w.sleep()
			waiter -= id
			
			if (!packageManager.canRequestPackageInstalls())
				throw IOException("No permission to request app installs")
		}
		
		val packageName = AndroidApk(apkfile).packageName
		
		val packageInstaller = packageManager.packageInstaller
		val params = SessionParams(SessionParams.MODE_FULL_INSTALL)
		
		val sessionId = packageInstaller.createSession(params)
		
		params.setAppPackageName(packageName)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) params.setInstallReason(PackageManager.INSTALL_REASON_USER)
		params.setSize(apkfile.length())
		
		packageInstaller.openSession(sessionId).use { session ->
			FileInputStream(apkfile).use { input ->
				session.openWrite(apkfile.name, 0, -1).use { out ->
					input.copyTo(out)
					
					out.flush()
					session.fsync(out)
				}
			}
			
			val intent = Intent(context, InstallReceiver::class.java)
//			intent.component = ComponentName(BuildConfig.APPLICATION_ID, Installer::class.java.name)
			intent.addFlags(FLAG_ACTIVITY_NEW_TASK)
			session.commit(PendingIntent.getBroadcast(context, id, intent, 0).intentSender)
		}
		
		log.log("commited session")
		
		val w = Waiter()
		w.timeout = 600_000
		waiter[id] = w
		w.sleep()
		waiter -= id
		
		val r: Bundle = result[id]!!
		result -= id
		
		val state = r.getInt(EXTRA_STATUS)
		if (state > STATUS_SUCCESS) {
			val message: String? = r.getString(EXTRA_STATUS_MESSAGE)
			
			throw IOException(message)
		}
	}
	
	@Throws(IOException::class)
	private fun installRoot(apkFile: File) {
		val result =
			Shell.su("cat ${apkFile.absolutePath} | pm install -r -i ${BuildConfig.APPLICATION_ID} -S ${apkFile.length()} --user current")
				.exec()
		
		val exitCode = result.code
		
		val output = result.out[0]
		if (exitCode == 0 && result.err.isNullOrEmpty() && output.toLowerCase().startsWith("success")) {
			return
		}
		
		log.log(result.toPrettyJson())
		
		when {
			output.startsWith("Failure") -> throw IOException(output.substring(output.indexOf('[') + 1, output.length - 2))
			output.startsWith("Error: ") -> throw IOException(output.substring(7))
			else -> throw IOException(output)
		}
	}
	
	init {
		// Configuration
		Shell.Config.setFlags(Shell.FLAG_REDIRECT_STDERR)
		Shell.Config.verboseLogging(false)
	}
}

class InstallReceiver : BroadcastReceiver() {
	override fun onReceive(context: Context, intent: Intent) {
		val extras = intent.extras ?: return
		
		val intent2 = extras.get(Intent.EXTRA_INTENT) as Intent?
		if (intent2 != null) {
			intent2.addFlags(FLAG_ACTIVITY_NEW_TASK)
			context.startActivity(intent2)
			context.sendBroadcast(intent2)
			return
		}
		
		val packageName = extras.getString(EXTRA_PACKAGE_NAME)
		if (packageName != null) {
			val id = AppList.findByPackageName(packageName)!!.getId()
			result[id] = extras
			waiter[id]?.wake()
			return
		}
	}
}

class InstallRequestActivity : Activity() {
	companion object {
		fun startRequestInstallPermissionDialog(id: Int) {
			val args = bundleOf("id" to id, "launch" to true)
			GlobalScope.launch(Main) {
				globalContext.startActivity(Intent(globalContext, InstallRequestActivity::class.java).apply {putExtras(args); addFlags(FLAG_ACTIVITY_NEW_TASK)})
			}
		}
	}
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		log.log("activity created")
		if(intent.getBooleanExtra("launch", false)) {
			val id = intent.getIntExtra("id", 0)
			intent.putExtra("launch", false)
			val request = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + BuildConfig.APPLICATION_ID))
			startActivityForResult(request, id)
			log.log("launched dialog")
		}
	}
	
	override fun onResume() {
		super.onResume()
		waiter[intent.getIntExtra("id", 0)]?.wake()
		finish()
		log.log("finished")
	}
	
	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		waiter[requestCode]?.wake()
		log.log("result=$resultCode")
		finish()
		log.log("finished")
	}
}
