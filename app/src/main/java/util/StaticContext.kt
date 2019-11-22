package de.binarynoise.appdate

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.net.ConnectivityManager
import android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.core.content.res.ResourcesCompat
import androidx.preference.PreferenceManager

val globalContext: Application
	@SuppressLint("PrivateApi") get() {
		val cls = Class.forName("android.app.ActivityThread")
		val method = cls.getMethod("currentApplication")
		val invoke = method.invoke(null)
		return invoke as Application
	}

fun getString(@StringRes resId: Int, vararg formatArgs: Any?): String =
	globalContext.getString(resId, *formatArgs)

@ColorInt
fun getColor(colorId: Int, theme: Resources.Theme): Int =
	ResourcesCompat.getColor(globalContext.resources, colorId, theme)

val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(globalContext)

fun hasInternetConnection(): Boolean {
	val connectivityManager = globalContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
	return connectivityManager.allNetworks.any {
		connectivityManager.getNetworkCapabilities(it)?.hasCapability(NET_CAPABILITY_INTERNET) ?: false
	}
}
