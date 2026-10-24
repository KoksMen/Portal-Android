package com.xxmrk888ytxx.portal.domain

interface WakeLockManager {
    fun acquireWakeLock(tag: String)
    fun releaseWakeLock(tag: String)
    fun startObserving()
}
