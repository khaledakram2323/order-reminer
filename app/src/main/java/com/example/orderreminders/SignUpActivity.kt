package com.example.orderreminders

import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.example.orderreminders.repository.OrderRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SignUpActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        auth = FirebaseAuth.getInstance()

        val btnThemeToggle = findViewById<ImageView>(R.id.btnThemeToggle)
        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

        // 1. Set the correct icon dynamically when the screen loads
        if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
            // App is in Dark Mode -> Show Sun icon to switch to Light Mode
            btnThemeToggle?.setImageResource(R.drawable.sun)
        } else {
            // App is in Light Mode -> Show Moon icon to switch to Dark Mode
            btnThemeToggle?.setImageResource(R.drawable.dark_mode)
        }

        // 2. Toggle the theme on click
        btnThemeToggle?.setOnClickListener {
            if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
        }

        val tilPharmacyName = findViewById<TextInputLayout>(R.id.tilPharmacyName)
        val etPharmacyName = findViewById<TextInputEditText>(R.id.etPharmacyName)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val etConfirmPassword = findViewById<TextInputEditText>(R.id.etConfirmPassword)
        val btnSignUp = findViewById<MaterialButton>(R.id.btnSignUp)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val tvLoginLink = findViewById<TextView>(R.id.tvLoginLink)

        // Programmatically toggle pharmacy icon between outline and fill on focus
        etPharmacyName.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                tilPharmacyName.endIconDrawable = ContextCompat.getDrawable(this, R.drawable.pharmacy_fill)
            } else {
                tilPharmacyName.endIconDrawable = ContextCompat.getDrawable(this, R.drawable.pharmacy)
            }
        }

        btnSignUp.setOnClickListener {
            val pharmacyName = etPharmacyName.text?.toString()?.trim().orEmpty()
            val password = etPassword.text?.toString()?.trim().orEmpty()
            val confirmPassword = etConfirmPassword.text?.toString()?.trim().orEmpty()

            if (pharmacyName.isEmpty()) {
                etPharmacyName.error = "يرجى إدخال اسم الصيدلية"
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                etPassword.error = "يرجى إدخال كلمة السر"
                return@setOnClickListener
            }

            if (password.length < 6) {
                etPassword.error = "كلمة السر يجب أن تكون 6 أحرف على الأقل"
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                etConfirmPassword.error = "كلمتا السر غير متطابقتين"
                return@setOnClickListener
            }

            val email = getPseudoEmail(pharmacyName)

            progressBar.visibility = View.VISIBLE
            btnSignUp.isEnabled = false

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val uid = auth.currentUser?.uid ?: ""

                        val profileMap = hashMapOf(
                            "pharmacyName" to pharmacyName,
                            "phone" to "",
                            "address" to "",
                            "mapLink" to ""
                        )

                        // Initialize profile in Firebase Firestore
                        FirebaseFirestore.getInstance().collection("Users").document(uid)
                            .set(profileMap, SetOptions.merge())
                            .addOnCompleteListener {
                                savePharmacyData(pharmacyName, "", "", "")

                                CoroutineScope(Dispatchers.IO).launch {
                                    val repository = OrderRepository(applicationContext)
                                    repository.migrateLegacyDataIfNecessary(pharmacyName)

                                    withContext(Dispatchers.Main) {
                                        progressBar.visibility = View.GONE
                                        val intent = Intent(this@SignUpActivity, MainActivity::class.java)
                                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        startActivity(intent)
                                        applyActivityTransition()
                                        finish()
                                    }
                                }
                            }
                    } else {
                        progressBar.visibility = View.GONE
                        btnSignUp.isEnabled = true
                        val errorMsg = task.exception?.localizedMessage ?: "فشل إنشاء الحساب"
                        Toast.makeText(this, "خطأ: $errorMsg", Toast.LENGTH_LONG).show()
                    }
                }
        }

        tvLoginLink.setOnClickListener {
            finish()
            applyActivityCloseTransition()
        }
    }

    private fun getPseudoEmail(pharmacyName: String): String {
        val sanitized = pharmacyName.trim().replace("\\s+".toRegex(), "")
        return "$sanitized@pharmacyapp.com"
    }

    private fun savePharmacyData(name: String, phone: String, address: String, mapLink: String = "") {
        val pharmacyPrefs = getSharedPreferences("PharmacyPrefs", MODE_PRIVATE)
        pharmacyPrefs.edit().clear().apply {
            putString("PHARMACY_NAME", name)
            putString("PHARMACY_PHONE", phone)
            putString("PHARMACY_ADDRESS", address)
            putString("PHARMACY_MAP_LINK", mapLink)
            putString("MAP_LINK", mapLink)
            apply()
        }

        val authPrefs = getSharedPreferences("AuthPrefs", MODE_PRIVATE)
        authPrefs.edit().clear().putString("pharmacyName", name).apply()
    }

    private fun applyActivityTransition() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, R.anim.slide_in_right, R.anim.fade_out)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out)
        }
    }

    private fun applyActivityCloseTransition() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, R.anim.fade_in, R.anim.slide_out_left)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.fade_in, R.anim.slide_out_left)
        }
    }
}
