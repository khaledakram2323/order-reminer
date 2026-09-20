package com.example.orderreminders

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.orderreminders.worker.ReminderWorker
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var ordersFragment: OrdersFragment
    private lateinit var addOrderFragment: AddOrderFragment
    private lateinit var archiveFragment: ArchiveFragment
    private lateinit var pharmacyDetailsFragment: PharmacyDetailsFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        if (FirebaseAuth.getInstance().currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        sharedPreferences = getSharedPreferences("ThemePrefs", MODE_PRIVATE)
        val isDarkMode = sharedPreferences.getBoolean("isDarkMode", false)
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.main)
        ) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)

            val isKeyboardVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            bottomNavigation.visibility = if (isKeyboardVisible) View.GONE else View.VISIBLE

            insets
        }

        if (savedInstanceState == null) {
            ordersFragment = OrdersFragment()
            addOrderFragment = AddOrderFragment()
            archiveFragment = ArchiveFragment()
            pharmacyDetailsFragment = PharmacyDetailsFragment()
            
            supportFragmentManager.beginTransaction().apply {
                add(R.id.fragment_container, pharmacyDetailsFragment, "pharmacy_details").hide(pharmacyDetailsFragment)
                add(R.id.fragment_container, archiveFragment, "archive").hide(archiveFragment)
                add(R.id.fragment_container, addOrderFragment, "add").hide(addOrderFragment)
                add(R.id.fragment_container, ordersFragment, "orders")
            }.commit()
        } else {
            // Recreated (e.g., after Theme toggle): restore existing fragments by tag
            ordersFragment = supportFragmentManager.findFragmentByTag("orders") as OrdersFragment
            addOrderFragment = supportFragmentManager.findFragmentByTag("add") as AddOrderFragment
            archiveFragment = supportFragmentManager.findFragmentByTag("archive") as ArchiveFragment
            pharmacyDetailsFragment = supportFragmentManager.findFragmentByTag("pharmacy_details") as PharmacyDetailsFragment
        }

        bottomNavigation.setOnItemSelectedListener { item ->
            val transaction = supportFragmentManager.beginTransaction()
            transaction.setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
            
            // Explicitly hide ALL fragments
            transaction.hide(ordersFragment)
            transaction.hide(addOrderFragment)
            transaction.hide(archiveFragment)
            transaction.hide(pharmacyDetailsFragment)

            // Show only the selected fragment
            when (item.itemId) {
                R.id.nav_orders -> transaction.show(ordersFragment)
                R.id.nav_add_order -> transaction.show(addOrderFragment)
                R.id.nav_archive -> transaction.show(archiveFragment)
                R.id.nav_pharmacy_details -> transaction.show(pharmacyDetailsFragment)
            }

            transaction.commit()
            true
        }

        if (savedInstanceState == null) {
            bottomNavigation.selectedItemId = R.id.nav_orders
        }

        setupReminders()
    }

    private fun setupReminders() {
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(
            15, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "OrderReminderWork",
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}
