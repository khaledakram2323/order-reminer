package com.example.orderreminders

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import com.example.orderreminders.utils.BrandingHelper
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class PharmacyDetailsFragment : Fragment() {

    private lateinit var sharedPrefs: SharedPreferences

    private lateinit var tvName: TextView
    private lateinit var tvPhone: TextView
    private lateinit var tvAddress: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_pharmacy_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        BrandingHelper.applyBranding(requireContext(), view)

        sharedPrefs = requireActivity().getSharedPreferences("PharmacyPrefs", Context.MODE_PRIVATE)

        val btnLogout = view.findViewById<ImageButton>(R.id.btnLogout)
        val btnEditSave = view.findViewById<ImageButton>(R.id.btnEditSave)
        val rowPharmacyPhone = view.findViewById<View>(R.id.rowPharmacyPhone)
        val btnOpenMaps = view.findViewById<MaterialButton>(R.id.btnOpenMaps)

        tvName = view.findViewById(R.id.tvPharmacyName)
        tvPhone = view.findViewById(R.id.tvPharmacyPhone)
        tvAddress = view.findViewById(R.id.tvPharmacyAddress)

        val themeSwitch = view.findViewById<SwitchCompat>(R.id.themeSwitch)
        val ivThemeIcon = view.findViewById<ImageView>(R.id.ivThemeIcon)

        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val isDarkMode = currentNightMode == Configuration.UI_MODE_NIGHT_YES

        themeSwitch?.isChecked = isDarkMode
        if (isDarkMode) {
            ivThemeIcon?.setImageResource(R.drawable.sun)
        } else {
            ivThemeIcon?.setImageResource(R.drawable.dark_mode)
        }

        themeSwitch?.setOnCheckedChangeListener { buttonView, isChecked ->
            if (buttonView.isPressed) {
                val themePrefs = requireActivity().getSharedPreferences("ThemePrefs", Context.MODE_PRIVATE)
                themePrefs.edit().putBoolean("isDarkMode", isChecked).apply()

                @Suppress("DEPRECATION")
                requireActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)

                if (isChecked) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                }
            }
        }

        // Load saved data locally and sync with Firebase
        loadPharmacyData()

        // Phone Click Listener -> Open Dialer
        rowPharmacyPhone?.setOnClickListener {
            val phoneNumber = tvPhone.text?.toString()?.trim().orEmpty()
            if (phoneNumber.isNotEmpty() && phoneNumber != "غير مسجل") {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
                startActivity(intent)
            } else {
                Toast.makeText(requireContext(), "رقم التليفون غير مسجل", Toast.LENGTH_SHORT).show()
            }
        }

        // Google Maps Button Click Listener -> Open Location on Maps Link
        btnOpenMaps?.setOnClickListener {
            val mapLink = sharedPrefs.getString("PHARMACY_MAP_LINK", "").orEmpty().ifBlank {
                sharedPrefs.getString("MAP_LINK", "").orEmpty()
            }
            if (mapLink.isNotBlank() && (mapLink.startsWith("http://", ignoreCase = true) || mapLink.startsWith("https://", ignoreCase = true))) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(mapLink))
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "تعذر فتح الرابط", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "برجاء إضافة رابط الموقع أولاً من خلال تعديل البيانات", Toast.LENGTH_LONG).show()
            }
        }

        // Logout Logic: Clear local SharedPreferences to prevent state bleeding between accounts
        btnLogout?.setOnClickListener {
            requireActivity().getSharedPreferences("PharmacyPrefs", Context.MODE_PRIVATE).edit().clear().apply()
            requireActivity().getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE).edit().clear().apply()

            FirebaseAuth.getInstance().signOut()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }

        // Open Dialog on Edit Button Click
        btnEditSave?.setOnClickListener {
            showEditPharmacyDialog()
        }
    }

    private fun loadPharmacyData() {
        val savedName = sharedPrefs.getString("PHARMACY_NAME", "صيدلية اليسر")
        val savedPhone = sharedPrefs.getString("PHARMACY_PHONE", "")
        val savedAddress = sharedPrefs.getString("PHARMACY_ADDRESS", "")

        tvName.text = if (savedName.isNullOrBlank()) "صيدلية اليسر" else savedName
        tvPhone.text = if (savedPhone.isNullOrBlank()) "غير مسجل" else savedPhone
        tvAddress.text = if (savedAddress.isNullOrBlank()) "غير مسجل" else savedAddress

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("Users").document(uid).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val dbName = snapshot.getString("pharmacyName")
                    val dbPhone = snapshot.getString("phone")
                    val dbAddress = snapshot.getString("address")
                    val dbMapLink = snapshot.getString("mapLink")

                    sharedPrefs.edit().apply {
                        if (!dbName.isNullOrBlank()) putString("PHARMACY_NAME", dbName)
                        if (!dbPhone.isNullOrBlank()) putString("PHARMACY_PHONE", dbPhone)
                        if (!dbAddress.isNullOrBlank()) putString("PHARMACY_ADDRESS", dbAddress)
                        if (!dbMapLink.isNullOrBlank()) {
                            putString("PHARMACY_MAP_LINK", dbMapLink)
                            putString("MAP_LINK", dbMapLink)
                        }
                        apply()
                    }

                    if (!dbName.isNullOrBlank()) tvName.text = dbName
                    if (!dbPhone.isNullOrBlank()) tvPhone.text = dbPhone
                    if (!dbAddress.isNullOrBlank()) tvAddress.text = dbAddress
                }
            }
    }

    private fun showEditPharmacyDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_pharmacy, null)

        val etEditPhone = dialogView.findViewById<TextInputEditText>(R.id.etEditPhone)
        val etEditAddress = dialogView.findViewById<TextInputEditText>(R.id.etEditAddress)
        val etMapLink = dialogView.findViewById<TextInputEditText>(R.id.etMapLink)
        val etCurrentPassword = dialogView.findViewById<TextInputEditText>(R.id.etCurrentPassword)
        val etNewPharmacyName = dialogView.findViewById<TextInputEditText>(R.id.etNewPharmacyName)
        val etNewPassword = dialogView.findViewById<TextInputEditText>(R.id.etNewPassword)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancel)
        val btnSaveDialog = dialogView.findViewById<MaterialButton>(R.id.btnSaveDialog)

        // Pre-fill phone, address, and map link
        val currentPhone = sharedPrefs.getString("PHARMACY_PHONE", "")
        val currentAddress = sharedPrefs.getString("PHARMACY_ADDRESS", "")
        val currentMapLink = sharedPrefs.getString("PHARMACY_MAP_LINK", "").orEmpty().ifBlank {
            sharedPrefs.getString("MAP_LINK", "")
        }
        etEditPhone.setText(currentPhone)
        etEditAddress.setText(currentAddress)
        etMapLink.setText(currentMapLink)

        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.Theme_OrderReminders_Dialog)
            .setView(dialogView)
            .show()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSaveDialog.setOnClickListener {
            val newPhone = etEditPhone.text?.toString()?.trim().orEmpty()
            val newAddress = etEditAddress.text?.toString()?.trim().orEmpty()
            val newMapLink = etMapLink.text?.toString()?.trim().orEmpty()
            val currentPassword = etCurrentPassword.text?.toString()?.trim().orEmpty()
            val newName = etNewPharmacyName.text?.toString()?.trim().orEmpty()
            val newPassword = etNewPassword.text?.toString()?.trim().orEmpty()

            val isAuthChangeRequested = newName.isNotEmpty() || newPassword.isNotEmpty()

            if (isAuthChangeRequested) {
                if (currentPassword.isEmpty()) {
                    etCurrentPassword.error = "يجب إدخال كلمة السر الحالية أولاً لتأكيد التعديل"
                    Toast.makeText(requireContext(), "يجب إدخال كلمة السر الحالية أولاً لتأكيد التعديل", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                if (newPassword.isNotEmpty() && newPassword.length < 6) {
                    etNewPassword.error = "كلمة السر الجديدة يجب أن تكون 6 أحرف على الأقل"
                    return@setOnClickListener
                }
            }

            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid == null) {
                Toast.makeText(requireContext(), "خطأ في حساب المستخدم، يرجى إعادة تسجيل الدخول", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSaveDialog.isEnabled = false

            val currentSavedName = sharedPrefs.getString("PHARMACY_NAME", "صيدلية اليسر") ?: "صيدلية اليسر"
            val finalName = if (newName.isNotEmpty()) newName else currentSavedName

            val profileMap = hashMapOf(
                "pharmacyName" to finalName,
                "phone" to newPhone,
                "address" to newAddress,
                "mapLink" to newMapLink
            )

            // 1. Write profile changes to Firebase Firestore
            FirebaseFirestore.getInstance().collection("Users").document(uid)
                .set(profileMap, SetOptions.merge())
                .addOnSuccessListener {
                    // Update local SharedPreferences
                    sharedPrefs.edit().apply {
                        putString("PHARMACY_NAME", finalName)
                        putString("PHARMACY_PHONE", newPhone)
                        putString("PHARMACY_ADDRESS", newAddress)
                        putString("PHARMACY_MAP_LINK", newMapLink)
                        putString("MAP_LINK", newMapLink)
                        apply()
                    }

                    val authPrefs = requireActivity().getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE)
                    authPrefs.edit().putString("pharmacyName", finalName).apply()

                    // Update UI
                    tvName.text = finalName
                    tvPhone.text = if (newPhone.isBlank()) "غير مسجل" else newPhone
                    tvAddress.text = if (newAddress.isBlank()) "غير مسجل" else newAddress

                    if (isAuthChangeRequested) {
                        val auth = FirebaseAuth.getInstance()
                        val currentUser = auth.currentUser
                        val currentEmail = currentUser?.email ?: ""

                        auth.signInWithEmailAndPassword(currentEmail, currentPassword).addOnCompleteListener { signInTask ->
                            if (signInTask.isSuccessful) {
                                val freshUser = auth.currentUser!!

                                val updatePasswordLogic = {
                                    if (newPassword.isNotEmpty()) {
                                        freshUser.updatePassword(newPassword).addOnCompleteListener { passTask ->
                                            if (passTask.isSuccessful) {
                                                Toast.makeText(context, "تم تحديث البيانات. يرجى تسجيل الدخول مجدداً", Toast.LENGTH_LONG).show()
                                                dialog.dismiss()

                                                auth.signOut()
                                                val intent = Intent(requireContext(), LoginActivity::class.java)
                                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                startActivity(intent)
                                                requireActivity().finish()
                                            } else {
                                                val errorMessage = passTask.exception?.localizedMessage ?: "Unknown"
                                                Log.e("AuthError", "Firebase Password Update Error: $errorMessage", passTask.exception)
                                                Toast.makeText(context, "فشل تحديث كلمة السر: $errorMessage", Toast.LENGTH_LONG).show()
                                                btnSaveDialog.isEnabled = true
                                            }
                                        }
                                    } else {
                                        Toast.makeText(context, "تم حفظ التعديلات بنجاح", Toast.LENGTH_SHORT).show()
                                        dialog.dismiss()
                                    }
                                }

                                if (newName.isNotEmpty()) {
                                    val sanitized = newName.trim().replace("\\s+".toRegex(), "")
                                    val newEmail = "$sanitized@pharmacyapp.com"

                                    freshUser.updateEmail(newEmail).addOnCompleteListener { emailTask ->
                                        if (emailTask.isSuccessful) {
                                            Toast.makeText(context, "تم تحديث اسم التسجيل بنجاح", Toast.LENGTH_SHORT).show()
                                        } else {
                                            val errorMessage = emailTask.exception?.localizedMessage ?: "Unknown"
                                            Log.e("AuthError", "Firebase Email Update Error: $errorMessage", emailTask.exception)
                                            Toast.makeText(context, "فشل تحديث الاسم: $errorMessage", Toast.LENGTH_LONG).show()
                                        }
                                        updatePasswordLogic()
                                    }
                                } else {
                                    updatePasswordLogic()
                                }
                            } else {
                                val errorMessage = signInTask.exception?.localizedMessage ?: "Unknown"
                                Log.e("AuthError", "Firebase Fresh Sign-In Error: $errorMessage", signInTask.exception)
                                btnSaveDialog.isEnabled = true
                                Toast.makeText(context, "كلمة السر الحالية غير صحيحة", Toast.LENGTH_LONG).show()
                            }
                        }
                    } else {
                        Toast.makeText(requireContext(), "تم حفظ التعديلات بنجاح", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                }
                .addOnFailureListener { e ->
                    btnSaveDialog.isEnabled = true
                    Log.e("PharmacyDetails", "Failed to save profile to Firebase", e)
                    Toast.makeText(requireContext(), "فشل حفظ البيانات: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
        }
    }
}
