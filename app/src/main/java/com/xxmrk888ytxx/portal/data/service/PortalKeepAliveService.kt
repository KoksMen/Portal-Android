package com.xxmrk888ytxx.portal.data.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.xxmrk888ytxx.coreandroid.fastDebugLog

class PortalKeepAliveService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        fastDebugLog("PortalKeepAliveService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        fastDebugLog("PortalKeepAliveService onStartCommand (START_STICKY)")
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        fastDebugLog("PortalKeepAliveService destroyed")
    }
}
