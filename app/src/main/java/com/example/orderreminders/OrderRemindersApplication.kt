package com.example.orderreminders

import android.app.Application
import com.google.firebase.FirebaseApp

class OrderRemindersApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}
