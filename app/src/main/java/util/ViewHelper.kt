package de.binarynoise.appdate

import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import kotlinx.coroutines.*

fun View.setVisibility(visible: Boolean) {
	let { v: View ->
		GlobalScope.launch(Dispatchers.Main) {
			v.visibility = if (visible) VISIBLE else GONE
		}
	}
}
