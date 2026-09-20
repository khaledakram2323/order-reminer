package com.example.orderreminders.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.orderreminders.MainActivity
import com.example.orderreminders.R

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val customerCode = intent.getStringExtra("CUSTOMER_CODE") ?: "غير معروف"
        val itemNames = intent.getStringExtra("ITEM_NAMES") ?: ""

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "order_reminders_channel",
                "تذكيرات الأوردرات",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "قناة لتذكير الأوردرات التي حان وقتها"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Intent to open the app when clicking the notification
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build the notification
        val largeIconBitmap = BitmapFactory.decodeResource(
            context.resources,
            R.mipmap.new_logo
        )

        val notification = NotificationCompat.Builder(context, "order_reminders_channel")
            .setSmallIcon(R.drawable.ic_notification_icon)
            .setLargeIcon(largeIconBitmap)
            .setColor(ContextCompat.getColor(context, R.color.brand_cyan))
            .setContentTitle("تذكير أوردر")
            .setContentText("العميل: $customerCode - الصنف: $itemNames")
            .setStyle(NotificationCompat.BigTextStyle().bigText("العميل: $customerCode\nالأصناف: $itemNames"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // Show the notification
        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notification)
    }
}
