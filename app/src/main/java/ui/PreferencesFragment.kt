package de.binarynoise.appdate

import android.os.Bundle
import androidx.navigation.fragment.findNavController
import androidx.preference.*

class PreferencesFragment : PreferenceFragmentCompat() {
	
	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		//TODO translate, move out constants
		val context = preferenceManager.context
		preferenceScreen = preferenceManager.createPreferenceScreen(context).apply {
			addPreference(PreferenceCategory(context)) {
				title = "Download and Install"
				
				addPreference(CheckBoxPreference(context)) {
					title = "Install after Download"
					key = Preferences.installAfterDownload
					isChecked = Preferences[key, false]
				}
			}
			
			addPreference(PreferenceCategory(context)) {
				title = "About"
				
				addPreference(Preference(context)) {
					title = "Licenses"
					summary = "View licenses of used libaries"
					setOnPreferenceClickListener {
						findNavController().navigate(R.id.nav_licenses)
						true
					}
				}
				
				addPreference(Preference(context)) {
					title = "About"
					summary = "More about this app"
					setOnPreferenceClickListener {
						findNavController().navigate(R.id.nav_aboutFragment)
						true
					}
				}
			}
			
			setIconSpaceReservedRecursively(false)
		}
	}
	
	override fun onDestroy() {
		preferenceScreen.removeOnClickListenersRecursively()
		preferenceScreen.removeAll()
		super.onDestroy()
	}
}

object Preferences {
	const val installAfterDownload = "install_after_download"
	
	inline operator fun <reified T> get(key: String, defVal: T): T {
		val sp = PreferenceManager.getDefaultSharedPreferences(globalContext)!!
		if (!sp.contains(key)) return defVal
		
		return when (T::class) {
			Boolean::class -> sp.getBoolean(key, false) as T
			String::class -> sp.getString(key, "") as T
			Int::class -> sp.getInt(key, 0) as T
			Float::class -> sp.getFloat(key, 0.0F) as T
			Long::class -> sp.getLong(key, 0L) as T
			else -> throw UnsupportedOperationException()
		}
	}
}

private fun PreferenceGroup.setIconSpaceReservedRecursively(iconSpaceReserved: Boolean) {
	isIconSpaceReserved = iconSpaceReserved
	for (i in 0 until preferenceCount) {
		val preference = getPreference(i)
		preference.isIconSpaceReserved = iconSpaceReserved
		if (preference is PreferenceGroup) preference.setIconSpaceReservedRecursively(iconSpaceReserved)
	}
}

private fun PreferenceGroup.removeOnClickListenersRecursively() {
	onPreferenceClickListener = null
	for (i in 0 until preferenceCount) {
		val preference = getPreference(i)
		preference.onPreferenceClickListener = null
		if (preference is PreferenceGroup) preference.removeOnClickListenersRecursively()
	}
}

inline fun <T : Preference> PreferenceGroup.addPreference(preference: T, setup: T.() -> Unit) {
	this.addPreference(preference)
	preference.apply(setup)
}
