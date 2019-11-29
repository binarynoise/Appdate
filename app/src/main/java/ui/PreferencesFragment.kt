package de.binarynoise.appdate

import android.os.Bundle
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup

class PreferencesFragment : PreferenceFragmentCompat() {
	
	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		//TODO translate, move out constants
		val context = preferenceManager.context
		preferenceScreen = preferenceManager.createPreferenceScreen(context).apply {
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
}

private fun PreferenceGroup.setIconSpaceReservedRecursively(iconSpaceReserved: Boolean) {
	isIconSpaceReserved = iconSpaceReserved
	for (i in 0 until preferenceCount) {
		val preference = getPreference(i)
		if (preference is PreferenceGroup) preference.setIconSpaceReservedRecursively(iconSpaceReserved)
	}
}

inline fun <T : Preference> PreferenceGroup.addPreference(preference: T, setup: T.() -> Unit) {
	this.addPreference(preference)
	preference.apply(setup)
}
