package com.xxmrk888ytxx.portal.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.net.Uri
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.xxmrk888ytxx.coreandroid.fastDebugLog
import com.xxmrk888ytxx.portal.R
import com.xxmrk888ytxx.portal.domain.AppIconManager
import com.xxmrk888ytxx.portal.view.mainActivity.MainActivity
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import kotlin.math.max

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

    private val customIconFile: File
        get() = File(context.filesDir, "custom_app_icon.png")

    override fun getCustomAppIconFile(): File? {
        return if (customIconFile.exists()) customIconFile else null
    }

    override fun setAppIcon(iconId: String) {
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
