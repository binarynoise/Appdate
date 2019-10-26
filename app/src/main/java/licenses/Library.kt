package de.binarynoise.appdate

class Library(val title: String, val url: String?, val license: License) : Comparable<Library> {
	
	override fun compareTo(other: Library): Int {
		if (this === other || super.equals(other)) return 0
		val i = license.licenseText.compareTo(other.license.licenseText)
		return if (i != 0) i else title.compareTo(other.title)
	}
	
	override fun equals(other: Any?): Boolean = other !is Library && compareTo(other as Library) == 0
}
