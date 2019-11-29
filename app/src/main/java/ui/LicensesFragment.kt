package de.binarynoise.appdate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mikepenz.aboutlibraries.LibsBuilder
import com.mikepenz.aboutlibraries.ui.LibsSupportFragment

class LicensesFragment : Fragment() {
	
	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
		inflater.inflate(R.layout.licenses_fragment, container, false)
	
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		
		val fragment =
			LibsBuilder().withFields(R.string::class.java.fields).withLicenseShown(true).withLicenseDialog(true)
				.supportFragment()
		
		activity?.run { supportFragmentManager.beginTransaction().add(R.id.license_list, fragment).commit() }
	}
	
	override fun onDestroyView() {
		activity?.run {
			val transaction = supportFragmentManager.beginTransaction()
			supportFragmentManager.fragments.forEach {
				if (it is LicensesFragment || it is LibsSupportFragment) transaction.remove(it)
			}
			transaction.commit()
		}
		
		super.onDestroyView()
	}
}
