package com.xxmrk888ytxx.coreandroid

fun fastDebugLog(m:String) {
    AndroidLogger.info(m)
}

fun fastDebugLog(m:Any?) {
    AndroidLogger.info(m)
}

fun fastDebugLog(m:Throwable) {
    AndroidLogger.info(m)
}