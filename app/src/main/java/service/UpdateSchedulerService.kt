package de.binarynoise.appdate

import android.app.job.JobParameters
import android.app.job.JobService
import kotlinx.coroutines.*

class UpdateSchedulerService : JobService() {
	private lateinit var jobParameters: JobParameters
	private var job: Job? = null
	
	override fun onStartJob(params: JobParameters): Boolean {
		jobParameters = params
		
		return if (job?.isActive == true) false
		else {
			GlobalScope.launch {
				job = async {
					checkForUpdates()
				}
			}
			true
		}
	}
	
	override fun onStopJob(params: JobParameters): Boolean {
		job?.cancel()
		return true
	}
	
	@RunInBackground
	private suspend fun checkForUpdates() {
		AppList.checkForUpdates()
		jobFinished(jobParameters, true)
	}
}
