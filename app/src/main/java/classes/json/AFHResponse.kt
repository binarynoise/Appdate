@file:Suppress("unused")

package de.binarynoise.appdate

@Suppress("PropertyName")
class AFHResponse(val MESSAGE: String, val CODE: String, val MIRRORS: Array<Mirror>) {
	private constructor() : this("", "", emptyArray())
}

class Mirror(val path: String, val name: String, val url: String) {
	private constructor() : this("", "", "")
}
