package com.xxmrk888ytxx.portal.domain

interface WatchDogAlarmController {
    fun scheduleFastCheck(intervalMillis: Long = 15_000L)
    fun scheduleAlarmClockCheck(intervalMillis: Long = 30_000L)
    fun cancelFastCheck()
    fun cancelAlarmClockCheck()
    fun cancelAll()
}
