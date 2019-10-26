package de.binarynoise.appdate

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.licenses_fragment.license_list as list

class LicensesFragment : Fragment() {
	
	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		// Inflate the layout for this fragment
		return inflater.inflate(R.layout.licenses_fragment, container, false)
	}
	
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		
		val adapter = MyAdapter(view.context)
		
		list.adapter = adapter
	}
	
	internal class MyAdapter(context: Context) : ArrayAdapter<Library>(context, -1, LIBRARIES) {
		
		override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
			val library = LIBRARIES[position]
			
			if (convertView != null && convertView.findViewById<TextView>(
					R.id.licenses_list_title
				).text == library.title
			) return convertView
			val view: View =
				convertView ?: LayoutInflater.from(context).inflate(R.layout.licenses_list_layout, parent, false)
			
			val title = view.findViewById<TextView>(R.id.licenses_list_title)
			title.text = library.title
			
			val url = view.findViewById<TextView>(R.id.licenses_list_url)
			if (library.url == null || library.url.isEmpty()) url.visibility = View.GONE
			else {
				url.text = library.url
				url.visibility = View.VISIBLE
			}
			
			val license = view.findViewById<TextView>(R.id.licenses_list_license)
			license.text = library.license.licenseText
			
			view.setOnClickListener {
				if (license.visibility == View.VISIBLE) license.visibility = View.GONE
				else license.visibility = View.VISIBLE
			}
			
			return view
		}
	}
	
	companion object {
		//TODO
		private val LIBRARIES: Array<Library> = arrayOf(
			Library("Android APK Parsing Lib", "https://github.com/joakime/android-apk-parser", License.APACHE2),
			Library("Android Jetpack", "https://developer.android.com/jetpack", License.APACHE2),
			Library("Google Material Design", null, License.APACHE2),
			Library("ApkParser", "https://github.com/joakime/android-apk-parser", License.APACHE2),
			Library(
				"Google OAuth",
				"https://developers.google.com/api-client-library/java/google-oauth-java-client/",
				License.APACHE2
			),
			Library("Google Sheets API", "https://developers.google.com/sheets/api/", License.APACHE2),
			Library("Jackson Kotlin Module", "https://github.com/FasterXML/jackson-module-kotlin", License.APACHE2),
			Library("Kotlin", "https://kotlinlang.org/", License.APACHE2),
			Library("libsu", "https://github.com/topjohnwu/libsu", License.APACHE2),
			Library(
				"Material Components For Android",
				"http://developer.android.com/tools/extras/support-library.html",
				License.APACHE2
			),
			Library("MaterialProgressBar", "https://github.com/zhanghai/MaterialProgressBar", License.APACHE2),
			Library("Semver4j", "https://github.com/vdurmont/semver4j", License.MIT)
		)
		
		init {
			
			LIBRARIES.sort()
		}
	}
}
