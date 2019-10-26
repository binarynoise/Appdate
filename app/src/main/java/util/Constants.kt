package de.binarynoise.appdate

object Constants {
	const val JOB_ID: Int = 123456789
	val APP_FILTER_PATTERN: Regex = "^((com\\.|org\\.)?(google|android|cyanogenmod|lineage)).*$".toRegex()
	const val PACKAGE_NAME: String = "packageName"
	const val PACKAGE_INFO: String = "packageInfo"
}
