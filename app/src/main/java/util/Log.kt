package de.binarynoise.appdate

import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log.*
import android.widget.Toast
import de.binarynoise.appdate.Log.Level.*
import de.binarynoise.appdate.Log.Place.Logcat

class Log constructor(private val tag: String? = null, val isDebug: Boolean = false) {
	private var hideDebugLog = true
	
	init {
		hideDebugLog = !(BuildConfig.DEBUG && isDebug)
	}
	
	@JvmOverloads
	fun log(message: CharSequence?, level: Level = Debug, vararg places: Place = arrayOf(Logcat)) {
		if (level == Debug && hideDebugLog || places.isEmpty()) return
		
		val logTag = tag ?: getCallingClass()
		for (place in places) when (place) {
			Logcat -> when (level) {
				Debug -> d(logTag, prefix + message)
				Warn -> w(logTag, prefix + message)
				Info -> i(logTag, prefix + message)
				Error -> e(logTag, prefix + message)
			}
			Place.Toast -> toast(message, if (level.ordinal >= Warn.ordinal) Toast.LENGTH_LONG else Toast.LENGTH_SHORT)
		}
	}
	
	@JvmOverloads
	fun log(message: CharSequence?, t: Throwable, level: Level = Debug, vararg places: Place = arrayOf(Logcat)) {
		if (level == Debug && hideDebugLog || places.isEmpty()) return
		for (place in places) when (place) {
			Logcat -> log("$message\n${getStackTraceString(t)}", level, Logcat)
			Place.Toast -> log("$message\n${t.localizedMessage}", level, Place.Toast)
		}
	}
	
	fun logSplit(message: String?, delimiter: String, head: String? = null, end: String? = null) {
		if (head != null) log(head)
		val intendention = if (head == null && end == null) "" else "\t"
		(message ?: "null").split(delimiter).forEach { log(intendention + it.trim()) }
		if (end != null) log(end) else if (head != null) log("end $head")
	}
	
	fun dumpBundle(bundle: Bundle?) {
		bundle?.keySet()?.forEach {
			log("$it -> ${bundle[it].toJson()}")
		}
	}
	
	enum class Level {
		Debug, Info, Warn, Error
	}
	
	enum class Place {
		Logcat, Toast;
		
		companion object {
			val all: Array<Place> get() = values()
		}
	}
	
	private fun getCallingClass(): String {
		val st = Thread.currentThread().stackTrace
		for (i in 5..st.size) {
			val sti = st[i]
			
			if (sti.className != this@Log.javaClass.name)
				return if (BuildConfig.DEBUG) sti.fileName + ":" + sti.lineNumber
				else sti.fileName
			else sti.className.split(".").last()
		}
		return "unknown"
	}
	
	companion object {
		private val prefix = if (BuildConfig.DEBUG) "appdate: " else ""
		
		private val handlerThread = HandlerThread("Util").apply { this.start() }
		private val handler = Handler(handlerThread.looper)
		
		private fun toast(text: CharSequence?, duration: Int) =
			handler.post { Toast.makeText(globalContext, text, duration).show() }
	}
}
