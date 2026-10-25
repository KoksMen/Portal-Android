package com.xxmrk888ytxx.portal.domain

import android.graphics.Bitmap
import java.io.File

interface AppIconManager {
    fun setAppIcon(iconId: String)
    fun processAndSaveCustomIcon(uriString: String): String?
    fun pinCustomAppShortcut(): Boolean
    fun getCustomAppIconFile(): File?
    fun getCurrentAppIconId(): String
    fun getAppIconBitmap(iconId: String? = null): Bitmap?
    fun getAppIconSmallIconRes(iconId: String? = null): Int
}
