package de.binarynoise.appdate

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.SimpleAdapter
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import de.binarynoise.appdate.Constants.PACKAGE_INFO
import de.binarynoise.appdate.Constants.PACKAGE_NAME
import de.binarynoise.appdate.Log.Level.*
import de.binarynoise.appdate.Log.Place.Companion.all
import de.binarynoise.appdate.Log.Place.Logcat
import de.binarynoise.appdate.Log.Place.Toast
import kotlinx.android.synthetic.main.add_app_fragment.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Main
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlinx.android.synthetic.main.add_app_fragment.addApp_addButton as addButton
import kotlinx.android.synthetic.main.add_app_fragment.addApp_appInstalledCheckbox as installedCheckBox
import kotlinx.android.synthetic.main.add_app_fragment.addApp_name as nameView
import kotlinx.android.synthetic.main.add_app_fragment.addApp_packageDetailsContainer as packageDetailContainer
import kotlinx.android.synthetic.main.add_app_fragment.addApp_packageNameSpinner as packagenamespinner
import kotlinx.android.synthetic.main.add_app_fragment.addApp_testButton as testButton
import kotlinx.android.synthetic.main.add_app_fragment.addApp_testButton_progressBar as testButtonProgressBar
import kotlinx.android.synthetic.main.add_app_fragment.addApp_url as urlView

class AddAppFragment : Fragment() {
	private var tempApp: App? = null
	private lateinit var tempInfo: PackageInfo
	
	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
		inflater.inflate(R.layout.add_app_fragment, container, false)
	
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		
		testButton.setOnClickListener { onTestButtonClick() }
		addButton.setOnClickListener { onAddButtonClick() }
		nameView.addTextChangedListener(object : TextChangedListener {
			override fun onTextChange(s: String) {
				tempApp?.run {
					if (s == installedName) return@run
					if (isInstalled()) nameView.setText(installedName)
					else installedName = s
				}
			}
		})
		
		urlView.addTextChangedListener(object : TextChangedListener {
			override fun onTextChange(s: String) {
				addButton.isEnabled = false
			}
		})
		
		packagenamespinner.isEnabled = false
		
		installedCheckBox.setOnCheckedChangeListener { _, checked ->
			this.onAppInstalledCheckbuttonCheckedChanged(checked)
		}
	}
	
	override fun onResume() {
		super.onResume()
		
		//show keyboard if text input fields are empty
		if (urlView.text?.length ?: 0 == 0) {
			urlView.isFocusableInTouchMode = true
			urlView.requestFocus()
			
			activity?.run {
				window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
				val systemService = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
				systemService.showSoftInput(urlView, 0)
			}
		}
		preloadInstalledAppLabels()
	}
	
	override fun onPause() {
		super.onPause()
		
		//hide keyboard
		activity?.run {
			window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
			val systemService = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
			systemService.hideSoftInputFromWindow(view!!.windowToken, 0)
		}
	}
	
	private fun onTestButtonClick() = GlobalScope.launch {
		withContext(Main) {
			testButton.isEnabled = false
			testButtonProgressBar.visibility = VISIBLE
		}
		
		var urlString = urlView.text.toString()
		urlString = urlFilterPattern.replace(urlString, "")
		
		val context = requireContext()
		if (urlString.isEmpty()) {
			log.log(getText(R.string.url_should_not_be_empty), Warn, Toast)
			withContext(Main) {
				testButtonProgressBar.visibility = INVISIBLE
				testButton.isEnabled = true
			}
			return@launch
		}
		
		if (!urlString.startsWith("http://") && !urlString.startsWith("https://") && !urlString.contains("://"))
			urlString = "https://$urlString"
		
		withContext(Main) { urlView?.setText(urlString) }
		
		try {
			URL(urlString)
		} catch (e: MalformedURLException) {
			log.log(context.getString(R.string.test_failed) + "\n" + getString(R.string.invalid_URL_could_not_add_app),
					Debug, Logcat, Toast)
			withContext(Main) {
				testButtonProgressBar?.visibility = INVISIBLE
				testButton?.isEnabled = true
			}
			return@launch
		}
		
		try {
			tempApp = if (installedCheckBox.isChecked) {
				App(tempInfo, context.packageManager, urlString)
			} else {
				val name = nameView.text.toString()
				App(name, urlString)
			}
		} catch (e: IOException) {
			log.log(context.getString(R.string.test_failed) + "\n" + e.localizedMessage, e, Error, *all)
			withContext(Main) {
				testButtonProgressBar.visibility = INVISIBLE
				testButton.isEnabled = true
			}
			return@launch
		}
		
		
		log.log(getString(R.string.test_successful_app_has_version_s, tempApp!!.updateVersion.toString()), Info, Toast)
		
		withContext(Main) {
			addButton.isEnabled = true
			testButtonProgressBar.visibility = INVISIBLE
			testButton.isEnabled = true
		}
	}
	
	private fun onAddButtonClick() = GlobalScope.launch {
		tempApp?.let { tempApp ->
			if (tempApp.installedName.isEmpty()) {
				log.log(getString(R.string.name_should_be_empty), Debug, Toast)
			} else {
				async { AppList.addToList(tempApp) }
				async {
					GlobalScope.launch(Main) {
						findNavController().navigateUp()
					}
				}
			}
		}
	}
	
	private fun onAppInstalledCheckbuttonCheckedChanged(checked: Boolean) {
		packagenamespinner.isEnabled = checked
		
		if (!checked) { // clear Spinner
			val spinnerAdapter =
				SimpleAdapter(globalContext, ArrayList<Map<String, String>>(), R.layout.layout_add_app_package_name_spinner,
						arrayOf(PACKAGE_NAME), intArrayOf(R.id.layout_addApp_packageName_spinner_text))
			packagenamespinner.adapter = spinnerAdapter
			
			packageDetailContainer.visibility = GONE
			return
		}
		
		// set up Spinner
		val context = requireContext()
		
		val pm = context.packageManager
		val installedPackages = pm.getInstalledPackages(0)
		
		installedPackages.sortWith(Comparator { o1, o2 -> getAppName(pm, o1).compareTo(getAppName(pm, o2)) })
		
		val hints = ArrayList<Map<String, Any>>()
		for (packageInfo in installedPackages) if (!Constants.APP_FILTER_PATTERN.matches(packageInfo.packageName)) {
			val map = HashMap<String, Any>()
			map[PACKAGE_INFO] = packageInfo
			map[PACKAGE_NAME] = pm.getApplicationLabel(packageInfo.applicationInfo).toString()
			hints.add(map)
		}
		
		if (hints.size == 1) log.log(getString(R.string.request_read_packages), Warn, Logcat, Toast)
		
		val spinnerAdapter =
			SimpleAdapter(context, hints, R.layout.layout_add_app_package_name_spinner, arrayOf(PACKAGE_NAME),
					intArrayOf(R.id.layout_addApp_packageName_spinner_text))
		
		packagenamespinner.adapter = spinnerAdapter
		
		val version = addApp_installed_version
		
		packagenamespinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
			override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
				val item = parent.selectedItem as Map<*, *>
				tempInfo = item[PACKAGE_INFO] as PackageInfo
				
				version.text = tempInfo.versionName
				nameView.setText(tempInfo.applicationInfo.loadLabel(pm))
			}
			
			override fun onNothingSelected(parent: AdapterView<*>) {}
		}
		
		packageDetailContainer.visibility = VISIBLE
	}
	
	companion object {
		private val packageNames = ConcurrentHashMap<String, CharSequence>()
		private val urlFilterPattern = "\\s".toRegex()
		private val log = Log()
		
		fun preloadInstalledAppLabels() {
			val thread = Thread {
				val pm = globalContext.packageManager
				val installedPackages = pm.getInstalledPackages(0)
				for (packageInfo in installedPackages) getAppName(pm, packageInfo)
			}
			thread.priority = Thread.MIN_PRIORITY
			thread.start()
		}
		
		private fun getAppName(pm: PackageManager, info: PackageInfo): String {
			if (!packageNames.containsKey(info.packageName)) packageNames[info.packageName] =
				info.applicationInfo.loadLabel(pm)
			return packageNames[info.packageName].toString()
		}
	}
}
