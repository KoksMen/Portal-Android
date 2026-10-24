package com.xxmrk888ytxx.portal.domain

import java.io.File

interface AppIconManager {
    fun setAppIcon(iconId: String)
    fun processAndSaveCustomIcon(uriString: String): String?
    fun pinCustomAppShortcut(): Boolean
    fun getCustomAppIconFile(): File?
}
