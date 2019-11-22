package de.binarynoise.appdate

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context.NOTIFICATION_SERVICE
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.O
import de.binarynoise.appdate.Notifier.Groups.UPDATE

object Notifier {
	
	fun initNotificationGroups() {
		registerGroup(UPDATE)
		// TODO register groups
	}
	
	fun notification(id: Int, notificationGroup: String, icon: Drawable, title: CharSequence, text: CharSequence,
			action: PendingIntent) {
		val context = globalContext
		
		val builder: Notification.Builder = if (SDK_INT >= O) Notification.Builder(context, notificationGroup)
		else @Suppress("DEPRECATION") Notification.Builder(context)
		with(builder) {
			setContentText(text)
			setAutoCancel(true)
			style = Notification.BigTextStyle().bigText(text)
			setLargeIcon((icon).toBitmap())
			setSmallIcon(R.drawable.ic_update_white_24dp)
			setContentTitle(title)
			setContentIntent(action)
		}
		val notification = builder.build()
		
		val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
		notificationManager.notify(id, notification)
	}
	
	private val notificationManager: NotificationManager by lazy {
		globalContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
	}
	
	fun clearNotification(id: Int) {
		notificationManager.cancel(id)
	}
	
	private fun registerGroup(notificationGroup: String) {
		if (SDK_INT < O) return
		notificationManager.createNotificationChannel(
				NotificationChannel(notificationGroup, notificationGroup, NotificationManager.IMPORTANCE_LOW))
	}
	
	object Groups {
		const val UPDATE: String = "update"
	}
}

private fun Drawable.toBitmap(): Bitmap {
	if (this is BitmapDrawable && bitmap != null) return bitmap
	
	val bitmap: Bitmap = if (intrinsicWidth <= 0 || intrinsicHeight <= 0)
		Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888) // Single color bitmap will be created of 1x1 pixel
	else Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)
	
	val canvas = Canvas(bitmap)
	setBounds(0, 0, canvas.width, canvas.height)
	draw(canvas)
	return bitmap
}
