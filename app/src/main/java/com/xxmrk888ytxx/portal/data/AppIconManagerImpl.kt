package com.xxmrk888ytxx.portal.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.xxmrk888ytxx.coreandroid.fastDebugLog
import com.xxmrk888ytxx.coreandroid.getActiveAppIconBitmap
import com.xxmrk888ytxx.portal.R
import com.xxmrk888ytxx.portal.domain.AppIconManager
import com.xxmrk888ytxx.portal.view.mainActivity.MainActivity
import com.xxmrk888ytxx.settingsscreen.model.AppIcon
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

class AppIconManagerImpl @Inject constructor(
    private val context: Context
) : AppIconManager {

    private val aliases = mapOf(
        "default" to "com.xxmrk888ytxx.portal.MainActivityDefault",
        "tactical" to "com.xxmrk888ytxx.portal.MainActivityTactical",
        "minimal" to "com.xxmrk888ytxx.portal.MainActivityMinimal",
        "soft" to "com.xxmrk888ytxx.portal.MainActivitySoft",
        "anime_1" to "com.xxmrk888ytxx.portal.MainActivityAnime1",
        "anime_2" to "com.xxmrk888ytxx.portal.MainActivityAnime2",
        "classic" to "com.xxmrk888ytxx.portal.MainActivityClassic",
        "anime_3" to "com.xxmrk888ytxx.portal.MainActivityAnime3"
    )

    @Volatile
    private var cachedIconId: String? = null

    private val customIconFile: File
        get() = File(context.filesDir, "custom_app_icon.png")

    override fun getCustomAppIconFile(): File? {
        return if (customIconFile.exists()) customIconFile else null
    }

    override fun getCurrentAppIconId(): String {
        cachedIconId?.let { return it }

        val packageManager = context.packageManager
        val packageName = context.packageName
        for ((id, aliasName) in aliases) {
            if (id == "default") continue
            try {
                val state = packageManager.getComponentEnabledSetting(ComponentName(packageName, aliasName))
                if (state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                    cachedIconId = id
                    return id
                }
            } catch (e: Exception) {
                // ignore
            }
        }
        cachedIconId = "default"
        return "default"
    }

    override fun setAppIcon(iconId: String) {
        cachedIconId = iconId
        val packageManager = context.packageManager
        val packageName = context.packageName
        val targetAlias = aliases[iconId] ?: aliases["default"]!!

        fastDebugLog("Changing app icon to $iconId (target alias: $targetAlias)")

        aliases.values.forEach { aliasName ->
            val component = ComponentName(packageName, aliasName)
            val desiredState = if (aliasName == targetAlias) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            try {
                val currentState = packageManager.getComponentEnabledSetting(component)
                if (currentState != desiredState) {
                    packageManager.setComponentEnabledSetting(
                        component,
                        desiredState,
                        PackageManager.DONT_KILL_APP
                    )
                }
            } catch (e: Exception) {
                fastDebugLog("Failed to set component state for $aliasName: ${e.message}")
            }
        }

        // Notify background services if running
        try {
            val updateIntent = Intent("com.xxmrk888ytxx.unlockservice.ACTION_UPDATE_NOTIFICATION").apply {
                setPackage(context.packageName)
            }
            val wifiIntent = Intent(updateIntent).setClassName(
                context.packageName,
                "com.xxmrk888ytxx.unlockservice.wifiService.WifiUnlockService"
            )
            context.startService(wifiIntent)
            val btIntent = Intent(updateIntent).setClassName(
                context.packageName,
                "com.xxmrk888ytxx.unlockservice.bluetoothService.BluetoothUnlockService"
            )
            context.startService(btIntent)
        } catch (e: Exception) {
            fastDebugLog("Failed to notify background services: ${e.message}")
        }
    }

    override fun getAppIconBitmap(iconId: String?): Bitmap? {
        return try {
            val id = iconId ?: getCurrentAppIconId()

            if (id == "custom") {
                getCustomAppIconFile()?.let { file ->
                    val customBmp = BitmapFactory.decodeFile(file.absolutePath)
                    if (customBmp != null) {
                        return scaleToNotificationSize(customBmp)
                    }
                }
            }

            // High-resolution pre-rendered squircle PNG preview from settings
            val preset = AppIcon.fromId(id)
            val previewBitmap = BitmapFactory.decodeResource(context.resources, preset.previewRes)
            if (previewBitmap != null) {
                return scaleToNotificationSize(previewBitmap)
            }

            // Fallback: use context.getActiveAppIconBitmap()
            context.getActiveAppIconBitmap()
        } catch (e: Exception) {
            fastDebugLog("Failed to getAppIconBitmap: ${e.message}")
            context.getActiveAppIconBitmap()
        }
    }

    override fun getAppIconSmallIconRes(iconId: String?): Int {
        val id = iconId ?: getCurrentAppIconId()
        return when (id) {
            "classic" -> R.drawable.ic_launcher_classic_foreground
            else -> R.drawable.ic_launcher_foreground
        }
    }

    private fun scaleToNotificationSize(original: Bitmap): Bitmap {
        val targetWidth = try {
            context.resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_width)
        } catch (_: Exception) {
            192
        }.coerceIn(128, 256)

        val targetHeight = try {
            context.resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_height)
        } catch (_: Exception) {
            192
        }.coerceIn(128, 256)

        return if (original.width != targetWidth || original.height != targetHeight) {
            Bitmap.createScaledBitmap(original, targetWidth, targetHeight, true)
        } else {
            original
        }
    }

    override fun processAndSaveCustomIcon(uriString: String): String? {
        return try {
            val uri = Uri.parse(uriString)
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val original = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            if (original == null) return null

            val squircleBitmap = com.xxmrk888ytxx.portal.utils.IconUtils.renderSquircleBitmap(original)
            FileOutputStream(customIconFile).use { out ->
                squircleBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            customIconFile.absolutePath
        } catch (e: Exception) {
            fastDebugLog("Failed to process custom icon: ${e.message}")
            null
        }
    }

    override fun pinCustomAppShortcut(): Boolean {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            fastDebugLog("Request pin shortcut is not supported by launcher")
            return false
        }
        val file = getCustomAppIconFile() ?: return false
        val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return false

        val targetIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        }

        val shortcutInfo = ShortcutInfoCompat.Builder(context, "portal_custom_launcher_shortcut")
            .setShortLabel(context.getString(R.string.app_name))
            .setLongLabel(context.getString(R.string.app_name))
            .setIcon(IconCompat.createWithBitmap(bitmap))
            .setIntent(targetIntent)
            .build()

        return ShortcutManagerCompat.requestPinShortcut(context, shortcutInfo, null)
    }
}
