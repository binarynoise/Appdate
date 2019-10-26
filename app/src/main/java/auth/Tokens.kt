package de.binarynoise.appdate

object Tokens {
	
	val googleCred: String by lazy { getGoogleCredNative() }
	
	init {
		System.loadLibrary("native-lib")
	}
	
	private external fun getGoogleCredNative(): String
}
