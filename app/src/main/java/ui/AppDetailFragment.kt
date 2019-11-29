package de.binarynoise.appdate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import de.binarynoise.appdate.Log.Level.*
import de.binarynoise.appdate.Log.Place.Companion.all
import de.binarynoise.appdate.Log.Place.Toast
import kotlinx.android.synthetic.main.app_detail_fragment.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Main
import java.io.IOException

class AppDetailFragment : Fragment() {
	
	private lateinit var app: App
	private val args: AppDetailFragmentArgs by navArgs()
	
	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		return inflater.inflate(R.layout.app_detail_fragment, container, false)
	}
	
	@Throws(IllegalArgumentException::class)
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		log.log("creating view")
		
		val id = args.id
		app = AppList.findById(id) ?: run { findNavController().navigateUp(); return }
		
		app.addChangeListener { updateUI() }
		app.addDownloadProgressListener { current, size ->
			GlobalScope.launch(Main) {
				downloadButton_progressBar?.run {
					isIndeterminate = current < 1000
					max = size
					progress = current
				}
			}
		}
		
		initUI()
		updateUI()
	}
	
	private fun initUI() {
		deleteButton.setOnClickListener { onDeleteButtonClick() }
		downloadButton.setOnClickListener { onDownloadButtonClick() }
		installButton.setOnClickListener { onInstallButtonClick() }
		updateButton.setOnClickListener { onUpdateButtonClick() }
		if(BuildConfig.DEBUG)
			debugView.setVisibility(true)
	}
	
	private fun onInstallButtonClick() = GlobalScope.launch {
		try {
			app.install()
		} catch (e: IOException) {
			log.log(getString(R.string.install_failed_s, app.installedName), e, Error, *all)
		}
	}
	
	private fun onUpdateButtonClick() = GlobalScope.launch {
		try {
			if (app.checkForUpdates()) {
				log.log(getString(R.string.update_available), Info, Toast)
			} else {
				log.log(getString(R.string.app_up_to_date), Info, Toast)
			}
		} catch (e: IOException) {
			log.log(getString(R.string.fetching_updates_failed_s), Warn, Toast)
		}
	}
	
	private fun onDownloadButtonClick() = GlobalScope.launch {
		downloadButton_progressBar.isIndeterminate = true
		try {
			app.download()
		} catch (e: IOException) {
			log.log(getString(R.string.download_failed_s, app.installedName), e, Warn, *all)
		}
	}
	
	private fun onDeleteButtonClick() = GlobalScope.launch(Dispatchers.IO) {
		val (navUp, fileDeleted) = app.delete()
		if (navUp) withContext(Main) { findNavController().navigateUp() }
		if (navUp || fileDeleted) log.log(getString(R.string.success))
	}
	
	private fun updateUI() = GlobalScope.launch(Main) {
		name.text = app.installedName
		icon.setImageDrawable(app.getIcon())
		if(BuildConfig.DEBUG) debugView.text = app.toPrettyJson()
		installedVersion.text = app.installedVersion.toString()
		availableVersion.text = app.updateVersion?.toString() ?: ""
		
		updateButtonRow()
		
		log.log("updated ui")
	}
	
	private suspend fun updateButtonRow() = withContext(Main) {
		val busy = app.downloading || app.installing || app.updating
		updateButton.isEnabled = !busy
		downloadButton.isEnabled = !busy && app.hasUpdates
				&& (!app.cacheFile.isValid() || app.cacheFile?.path?.endsWith(app.updateVersion.toString()) != true)
		installButton.isEnabled = !busy && app.cacheFile.isValid() && app.hasUpdates
		deleteButton.isEnabled = !busy && (app.cacheFile != null || !app.isInstalled())
		
		downloadButton_progressBar.setVisibility(app.downloading)
		installButton_progressBar.setVisibility(app.installing)
		updateButton_progressBar.setVisibility(app.updating)
	}
	
	companion object {
		private val log = Log(isDebug = false)
	}
}
