package de.binarynoise.appdate

import android.text.Editable
import android.text.TextWatcher

interface TextChangedListener : TextWatcher {
	override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
	
	override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
		onTextChange(s.toString())
	}
	
	override fun afterTextChanged(s: Editable) {}
	
	fun onTextChange(s: String)
}
