package com.xxmrk888ytxx.settingsscreen.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.xxmrk888ytxx.settingsscreen.R

enum class AppIcon(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    @DrawableRes val previewRes: Int
) {
    DEFAULT(
        id = "default",
        titleRes = R.string.app_icon_anime_1_title,
        descriptionRes = R.string.app_icon_anime_1_desc,
        previewRes = R.drawable.app_icon_anime_1_preview
    ),
    ANIME_2(
        id = "anime_2",
        titleRes = R.string.app_icon_anime_2_title,
        descriptionRes = R.string.app_icon_anime_2_desc,
        previewRes = R.drawable.app_icon_anime_2_preview
    ),
    CLASSIC(
        id = "classic",
        titleRes = R.string.app_icon_classic_title,
        descriptionRes = R.string.app_icon_classic_desc,
        previewRes = R.drawable.app_icon_classic_preview
    );

    companion object {
        fun fromId(id: String?): AppIcon = entries.find { it.id == id } ?: DEFAULT
    }
}
