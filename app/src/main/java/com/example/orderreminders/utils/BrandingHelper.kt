package com.example.orderreminders.utils

import android.content.Context
import android.view.View
import android.widget.ImageView
import com.example.orderreminders.R

object BrandingHelper {

    fun applyBranding(context: Context, view: View) {
        val sharedPrefs = context.getSharedPreferences("PharmacyPrefs", Context.MODE_PRIVATE)
        val authPrefs = context.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE)

        val pharmacyName = (sharedPrefs.getString("PHARMACY_NAME", "")
            ?.ifEmpty { authPrefs.getString("pharmacyName", "") }
            ?: authPrefs.getString("pharmacyName", "") ?: "").trim()

        val topLogo = view.findViewById<ImageView>(R.id.btnPharmacyDetails)

        // البحث عن الووتر مارك بكل الأسماء المحتملة في التطبيق
        var watermark = view.findViewById<ImageView>(R.id.ivBackgroundWatermark)
        if (watermark == null) {
            watermark = view.findViewById<ImageView>(R.id.ivAuthWatermark) // بتاع اللوجن والريجيستر
        }
        if (watermark == null) {
            val watermarkId = context.resources.getIdentifier("ivWatermark", "id", context.packageName)
            if (watermarkId != 0) {
                watermark = view.findViewById(watermarkId)
            }
        }

        // مسح أي صور قديمة لمنع التداخل
        topLogo?.setImageDrawable(null)
        topLogo?.background = null
        watermark?.setImageDrawable(null)
        watermark?.background = null

        val scale = context.resources.displayMetrics.density
        val originalSize = (42 * scale + 0.5f).toInt()
        val enlargedSize = (64 * scale + 1.0f).toInt()

        if (pharmacyName == "اليسر" || pharmacyName == "يسر") {
            // صيدلية اليسر
            topLogo?.setImageResource(R.drawable.yosr_watermark)
            watermark?.setImageResource(R.drawable.yosr_watermark)

            topLogo?.layoutParams?.width = originalSize
            topLogo?.layoutParams?.height = originalSize
            topLogo?.requestLayout()

            watermark?.scaleX = 1.0f
            watermark?.scaleY = 1.0f
        } else {
            // أي حساب تاني أو لو لسه مسجلش دخول
            topLogo?.setImageResource(R.drawable.logolight)
            watermark?.setImageResource(R.drawable.logolight)

            topLogo?.layoutParams?.width = enlargedSize
            topLogo?.layoutParams?.height = enlargedSize
            topLogo?.requestLayout()

            // تكبير الووتر مارك
            watermark?.scaleX = 1.8f
            watermark?.scaleY = 1.8f
        }
    }
}