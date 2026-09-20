package com.example.orderreminders.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object AlarmScheduler {

    fun scheduleReminder(
        context: Context,
        reminderTime: Long,
        customerCode: String,
        itemNames: String
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Create the Intent pointing to the BroadcastReceiver
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("CUSTOMER_CODE", customerCode)
            putExtra("ITEM_NAMES", itemNames)
        }

        // Use a unique request code so alarms don't overwrite each other
        val requestCode = reminderTime.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Schedule the exact alarm
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ requires checking if we can schedule exact alarms
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminderTime,
                    pendingIntent
                )
            } else {
                // Fallback to inexact alarm if the user revoked the permission
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminderTime,
                    pendingIntent
                )
            }
        } else {
            // Android 11 and below
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminderTime,
                pendingIntent
            )
        }
    }
}
