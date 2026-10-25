package com.xxmrk888ytxx.coreandroid

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.getSystemService
import kotlinx.coroutines.CancellationException

inline fun Context.buildNotificationChannel(
    id: String,
    name: String,
    configuration: NotificationChannel.() -> Unit = {}
) {
    val channel =
        NotificationChannel(id, name, NotificationManager.IMPORTANCE_DEFAULT).apply(configuration)

    val notificationManager = getSystemService<NotificationManager>()

    notificationManager?.createNotificationChannel(channel)
}

fun Context.buildForegroundServiceNotificationChannel(
    id: String,
    name: String,
) = buildNotificationChannel(id, name) {
    importance = IMPORTANCE_LOW
}

inline fun Context.buildNotification(
    channelId: String,
    configuration: Notification.Builder.() -> Unit
): Notification {
    val notificationBuilder = Notification.Builder(this, channelId)

    return notificationBuilder.apply(configuration).build()
}

fun Context.getActiveAppIconBitmap(width: Int = 192, height: Int = 192): Bitmap? {
    return try {
        val targetWidth = try {
            resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_width)
        } catch (_: Exception) {
            width
        }.coerceIn(128, 256)

        val targetHeight = try {
            resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_height)
        } catch (_: Exception) {
            height
        }.coerceIn(128, 256)

        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            setPackage(packageName)
        }
        val resolveInfo = pm.queryIntentActivities(intent, 0).firstOrNull()
        val drawable = resolveInfo?.loadIcon(pm) ?: applicationInfo.loadIcon(pm) ?: return null

        val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, targetWidth, targetHeight)
        drawable.draw(canvas)
        bitmap
    } catch (e: Exception) {
        null
    }
}

inline fun saveCall(isPrintToDebug: Boolean = true, block: () -> Unit) {
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        if (isPrintToDebug)
            fastDebugLog(e)
    }
}

fun String.formatToMacAddress(): String? {
    val cleaned = this.filter { it.isDigit() || it.lowercaseChar() in 'a'..'f' }

    if (cleaned.length != 12) return null

    val sb = StringBuilder()
    for (i in cleaned.indices) {
        if (i > 0 && i % 2 == 0) {
            sb.append(":")
        }
        sb.append(cleaned[i].uppercaseChar())
    }

    return sb.toString()
}
