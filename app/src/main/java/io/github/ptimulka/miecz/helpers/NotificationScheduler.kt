package io.github.ptimulka.miecz.helpers

import android.content.Context

interface NotificationScheduler {
    fun scheduleDailyNotification(hour: Int, minute: Int)
    fun cancelDailyNotification()
}

class AndroidNotificationScheduler(private val context: Context) : NotificationScheduler {
    override fun scheduleDailyNotification(hour: Int, minute: Int) {
        NotificationHelper.scheduleDailyNotification(context, hour, minute)
    }

    override fun cancelDailyNotification() {
        NotificationHelper.cancelDailyNotification(context)
    }
}
