package de.binarynoise.appdate

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import de.binarynoise.appdate.Constants.APP_FILTER_PATTERN
import de.binarynoise.appdate.Constants.JOB_ID
import de.binarynoise.appdate.Log.Level.Warn
import de.binarynoise.appdate.Log.Place.Logcat
import de.binarynoise.appdate.Log.Place.Toast
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

object Init {
	@Volatile
	private var hasDoneScheduleBackgroundUpdate = false
	private val log = Log(isDebug = false)
	
	fun scheduleBackgroundUpdate() {
		if (hasDoneScheduleBackgroundUpdate) return
		hasDoneScheduleBackgroundUpdate = true
		
		val context = globalContext
		
		with(JobInfo.Builder(JOB_ID, ComponentName(context, UpdateSchedulerService::class.java))) {
			setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED) // TODO
			setBackoffCriteria(JobInfo.MAX_BACKOFF_DELAY_MILLIS, JobInfo.BACKOFF_POLICY_EXPONENTIAL)
			setPersisted(false)
			
			val duration = 4L // TODO
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) setPeriodic(
				TimeUnit.HOURS.toMillis(duration), TimeUnit.MINUTES.toMillis(duration * 10)
			)
			else setPeriodic(TimeUnit.HOURS.toMillis(duration))
			
			(context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler).schedule(build())
		}
	}
	
	fun checkPermissions() {
		val context = globalContext
		val userPackages =
			context.packageManager.getInstalledPackages(0).filterNot { APP_FILTER_PATTERN.matches(it.packageName) }
		
		if (userPackages.size <= 1) {
			log.log(getString(R.string.request_read_packages), Warn, Logcat, Toast)
			
			// most likely the permission isn't "granted" by XPrivacyLua, so we'll try to open it
			// that the user can revoke the restriction
			
			with(Intent(Intent.ACTION_MAIN)) {
				component = ComponentName.unflattenFromString("eu.faircode.xlua/.ActivityMain")
				addCategory(Intent.CATEGORY_LAUNCHER)
				putExtra("package", BuildConfig.APPLICATION_ID)
				addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
				context.startActivity(this)
			}
		}
	}
}
