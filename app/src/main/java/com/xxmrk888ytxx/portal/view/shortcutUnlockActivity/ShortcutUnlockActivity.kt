package com.xxmrk888ytxx.portal.view.shortcutUnlockActivity

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class ShortcutUnlockActivity @Inject constructor(
    private val shortcutUnlockActivityViewModelFactory: ShortcutUnlockActivityViewModel.Factory
) : FragmentActivity() {

    private val shortcutUnlockActivityViewModel by viewModels<ShortcutUnlockActivityViewModel> { shortcutUnlockActivityViewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
        super.onCreate(savedInstanceState)

        window.setBackgroundDrawableResource(android.R.color.transparent)
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)

        lifecycleScope.launch {
            shortcutUnlockActivityViewModel.onFinishEvent.collect {
                finish()
            }
        }

        shortcutUnlockActivityViewModel.requestUnlock(this, intent)
    }

    override fun finish() {
        super.finish()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }

    companion object {
        const val UNLOCK_FROM_SHORTCUT_ACTION: String = "com.xxmrk888ytxx.portal.UNLOCK_FROM_SHORTCUT_ACTION"
        const val SHORTCUT_ID_EXTRA = "shortcutId"
    }
}
