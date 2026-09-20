package com.example.orderreminders.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.orderreminders.MainActivity
import com.example.orderreminders.R
import com.example.orderreminders.repository.OrderRepository

class ReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val repository = OrderRepository(applicationContext)
        
        val currentTime = System.currentTimeMillis()
        val ordersToNotify = repository.getOrdersThatNeedNotification(currentTime)

        if (ordersToNotify.isNotEmpty()) {
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            createNotificationChannel(notificationManager)

            ordersToNotify.forEach { order ->
                showNotification(notificationManager, order.id, order.customerCode)
                repository.markNotificationSent(order.id)
            }
        }

        return Result.success()
    }

    private fun createNotificationChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "تذكيرات الأوردرات",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "قناة لتذكير الأوردرات التي حان وقتها"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(notificationManager: NotificationManager, orderId: String, customerCode: String) {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            orderId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val largeIconBitmap = BitmapFactory.decodeResource(
            applicationContext.resources,
            R.mipmap.new_logo
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_icon)
            .setLargeIcon(largeIconBitmap)
            .setColor(ContextCompat.getColor(applicationContext, R.color.brand_cyan))
            .setContentTitle("تذكير أوردر")
            .setContentText("حان موعد أوردر العميل: $customerCode")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(orderId.hashCode(), notification)
    }

    companion object {
        const val CHANNEL_ID = "order_reminders_channel"
    }
}
