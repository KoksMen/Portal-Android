package com.xxmrk888ytxx.portal.domain

interface KeepAliveManager {
    fun startObserving()
    fun ensureKeepAliveRunning()
}
