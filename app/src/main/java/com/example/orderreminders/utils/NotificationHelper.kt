package com.example.orderreminders.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.orderreminders.R

object NotificationHelper {

    private const val CHANNEL_ID = "new_orders_channel"
    private const val CHANNEL_NAME = "أوردرات جديدة"
    private const val CHANNEL_DESC = "إشعارات عند إضافة أوردر جديد"

    fun showNewOrderNotification(context: Context, customerCode: String, itemName: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
            }
            notificationManager.createNotificationChannel(channel)
        }
        val largeIcon = BitmapFactory.decodeResource(context.resources, R.mipmap.new_logo)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_icon)
            .setLargeIcon(largeIcon)
            .setColor(ContextCompat.getColor(context, R.color.brand_cyan))
            .setContentTitle("أوردر جديد!")
            .setContentText("تم إضافة طلب جديد للعميل: $customerCode - الصنف: $itemName")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), notification)
            }
        } else {
            NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), notification)
        }
    }
}