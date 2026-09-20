package com.example.orderreminders

import android.Manifest
import android.app.DatePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.orderreminders.data.OrderItemEntity
import com.example.orderreminders.receiver.AlarmScheduler
import com.example.orderreminders.utils.BrandingHelper
import com.example.orderreminders.utils.NotificationHelper
import com.example.orderreminders.viewmodel.OrderViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

class AddOrderFragment : Fragment() {

    private lateinit var viewModel: OrderViewModel

    private lateinit var etCustomerCode: EditText
    private lateinit var etPhone: EditText
    private lateinit var etNotes: EditText

    private lateinit var btnToggleOrderType: MaterialButton
    private lateinit var togglePriorityGroup: MaterialButtonToggleGroup
    private lateinit var btnPriorityNormal: Button
    private lateinit var btnPriorityUrgent: Button
    private lateinit var btnPriorityShortage: Button

    private lateinit var tvSelectedDate: EditText
    private lateinit var etHour: EditText
    private lateinit var etMinute: EditText
    private lateinit var toggleAmPm: MaterialButtonToggleGroup
    private lateinit var btnAm: Button
    private lateinit var btnPm: Button
    private lateinit var itemsContainer: LinearLayout

    private var reminderTime: Long? = null
    
    private var selectedYear: Int = 0
    private var selectedMonth: Int = 0
    private var selectedDay: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_order, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        BrandingHelper.applyBranding(requireContext(), view)

        // Request Notification Permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        viewModel = ViewModelProvider(requireActivity())[OrderViewModel::class.java]

        // Views
        etCustomerCode = view.findViewById(R.id.etCustomerCode)
        etPhone = view.findViewById(R.id.etPhone)
        etNotes = view.findViewById(R.id.etNotes)

        btnToggleOrderType = view.findViewById(R.id.btnToggleOrderType)
        togglePriorityGroup = view.findViewById(R.id.togglePriorityGroup)
        btnPriorityNormal = view.findViewById(R.id.btnPriorityNormal)
        btnPriorityUrgent = view.findViewById(R.id.btnPriorityUrgent)
        btnPriorityShortage = view.findViewById(R.id.btnPriorityShortage)

        tvSelectedDate = view.findViewById(R.id.tvSelectedDate)
        etHour = view.findViewById(R.id.etHour)
        etMinute = view.findViewById(R.id.etMinute)
        toggleAmPm = view.findViewById(R.id.toggleAmPm)
        btnAm = view.findViewById(R.id.btnAm)
        btnPm = view.findViewById(R.id.btnPm)
        itemsContainer = view.findViewById(R.id.itemsContainer)

        // Order Type Toggle Logic
        btnToggleOrderType.setOnClickListener {
            if (btnToggleOrderType.text == "توصيل") {
                btnToggleOrderType.text = "استلام"
                btnToggleOrderType.setIconResource(R.drawable.hand_package)
            } else {
                btnToggleOrderType.text = "توصيل"
                btnToggleOrderType.setIconResource(R.drawable.shipping)
            }
        }

        // اختيار التاريخ والوقت
        view.findViewById<View>(R.id.tilPickDate).setOnClickListener {
            chooseDate()
        }
        tvSelectedDate.setOnClickListener {
            chooseDate()
        }

        // إضافة صنف
        view.findViewById<Button>(R.id.btnAddItem).setOnClickListener {
            addItemView()
        }

        view.findViewById<Button>(R.id.btnSave).setOnClickListener {
            saveOrder()
        }

        // Delay heavy initialization to fix transition lag
        viewLifecycleOwner.lifecycleScope.launch {
            // Set current date as default
            val calendar = Calendar.getInstance()
            selectedYear = calendar.get(Calendar.YEAR)
            selectedMonth = calendar.get(Calendar.MONTH)
            selectedDay = calendar.get(Calendar.DAY_OF_MONTH)
            tvSelectedDate.setText(String.format(Locale.getDefault(), "%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear))
            
            // إضافة صنف أول بشكل تلقائي
            addItemView()
        }
    }

    // =========================================================
    // اختيار التاريخ
    // =========================================================

    private fun chooseDate() {
        DatePickerDialog(
            requireContext(),
            R.style.Theme_OrderReminders_Dialog,
            { _, year, month, dayOfMonth ->
                selectedYear = year
                selectedMonth = month
                selectedDay = dayOfMonth
                
                tvSelectedDate.setText(String.format(
                    Locale.getDefault(),
                    "%02d/%02d/%04d",
                    dayOfMonth,
                    month + 1,
                    year
                ))
            },
            selectedYear,
            selectedMonth,
            selectedDay
        ).show()
    }

    // =========================================================
    // إضافة صنف جديد
    // =========================================================

    private fun addItemView() {

        val itemView = layoutInflater.inflate(
            R.layout.item_order_input,
            itemsContainer,
            false
        )

        val rgAvailability = itemView.findViewById<MaterialAutoCompleteTextView>(R.id.spinnerAvailability)
        val tilWarehouse = itemView.findViewById<TextInputLayout>(R.id.tilWarehouse)
        val warehouse = itemView.findViewById<EditText>(R.id.etWarehouse)

        // Setup Dropdown Options
        val options = arrayOf("متوفرة", "سيتم الطلب من المخزن", "تم الطلب من المخزن")
        val adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown_status, options)
        rgAvailability.setAdapter(adapter)

        rgAvailability.setOnItemClickListener { _, _, position, _ ->
            if (options[position] == "تم الطلب من المخزن") {
                tilWarehouse.visibility = View.VISIBLE
            } else {
                tilWarehouse.visibility = View.GONE
                warehouse.setText("")
            }
        }

        // زر حذف الصنف
        itemView.findViewById<Button>(R.id.btnDeleteItem).setOnClickListener {
            itemsContainer.removeView(itemView)
        }

        itemsContainer.addView(itemView)
    }

    // =========================================================
    // حفظ الأوردر
    // =========================================================

    private fun saveOrder() {

        val customerCode = etCustomerCode.text.toString().trim()

        if (customerCode.isEmpty()) {
            etCustomerCode.error = "كود العميل مطلوب"
            etCustomerCode.requestFocus()
            return
        }

        val type = if (btnToggleOrderType.text == "توصيل") "DELIVERY" else "PICKUP"

        val priority = when (togglePriorityGroup.checkedButtonId) {
            R.id.btnPriorityUrgent -> "URGENT"
            R.id.btnPriorityShortage -> "SHORTAGE"
            else -> "NORMAL"
        }

        val items = mutableListOf<OrderItemEntity>()

        for (i in 0 until itemsContainer.childCount) {

            val itemView = itemsContainer.getChildAt(i)

            val itemName = itemView.findViewById<EditText>(R.id.etItemName)
            val rgAvailability = itemView.findViewById<MaterialAutoCompleteTextView>(R.id.spinnerAvailability)
            val warehouse = itemView.findViewById<EditText>(R.id.etWarehouse)

            val name = itemName.text.toString().trim()

            val availabilityStr = rgAvailability.text.toString()
            val availability = when (availabilityStr) {
                "متوفرة", "متوفر" -> "AVAILABLE"
                "سيتم الطلب من المخزن" -> "TO_BE_ORDERED"
                "تم الطلب من المخزن" -> "ORDERED"
                else -> "AVAILABLE"
            }

            val warehouseName = warehouse.text.toString().trim()

            if (name.isNotEmpty()) {

                val finalWarehouse = if (availability == "ORDERED") warehouseName else ""

                items.add(
                    OrderItemEntity(
                        orderId = "",
                        name = name,
                        availability = availability,
                        warehouseName = finalWarehouse
                    )
                )
            }
        }

        if (items.isEmpty()) {
            Toast.makeText(requireContext(), "أضف صنف واحد على الأقل", Toast.LENGTH_SHORT).show()
            return
        }

        for (item in items) {
            if (item.availability == "ORDERED" && item.warehouseName.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "يجب كتابة اسم المخزن للأصناف التي تم طلبها",
                    Toast.LENGTH_LONG
                ).show()
                return
            }
        }

        val strHour = etHour.text.toString().trim()
        val strMinute = etMinute.text.toString().trim()
        
        if (strHour.isNotEmpty() || strMinute.isNotEmpty()) {
            if (strHour.isEmpty()) {
                etHour.error = "مطلوب"
                etHour.requestFocus()
                return
            }
            if (strMinute.isEmpty()) {
                etMinute.error = "مطلوب"
                etMinute.requestFocus()
                return
            }
            
            var hour = strHour.toIntOrNull() ?: 0
            val minute = strMinute.toIntOrNull() ?: 0
            
            if (hour < 1 || hour > 12) {
                etHour.error = "1 - 12"
                etHour.requestFocus()
                return
            }
            if (minute < 0 || minute > 59) {
                etMinute.error = "0 - 59"
                etMinute.requestFocus()
                return
            }
            
            val isPm = toggleAmPm.checkedButtonId == R.id.btnPm
            if (isPm && hour != 12) {
                hour += 12
            } else if (!isPm && hour == 12) {
                hour = 0
            }
            
            val calendar = Calendar.getInstance()
            calendar.set(selectedYear, selectedMonth, selectedDay, hour, minute, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            reminderTime = calendar.timeInMillis
        } else {
            reminderTime = null
        }

        val notes = etNotes.text.toString().trim()

        view?.findViewById<Button>(R.id.btnSave)?.isEnabled = false

        lifecycleScope.launch {
            val isSuccess = viewModel.addOrder(
                type = type,
                customerCode = customerCode,
                phone = etPhone.text.toString().trim(),
                priority = priority,
                reminderTime = reminderTime,
                items = items,
                notes = notes
            )

            if (isSuccess) {
                // Schedule local push notification if a time was selected
                if (reminderTime != null && reminderTime!! > System.currentTimeMillis()) {
                    val itemNamesStr = items.joinToString(", ") { it.name }
                    AlarmScheduler.scheduleReminder(
                        context = requireContext(),
                        reminderTime = reminderTime!!,
                        customerCode = customerCode,
                        itemNames = itemNamesStr
                    )
                }

                // Trigger instant notification for the new order creation locally
                val firstItemName = items.firstOrNull()?.name ?: "عنصر غير معروف"
                NotificationHelper.showNewOrderNotification(
                    requireContext(),
                    customerCode,
                    firstItemName
                )

                Toast.makeText(requireContext(), "تم حفظ الأوردر بنجاح", Toast.LENGTH_SHORT).show()
                requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigation).selectedItemId = R.id.nav_orders
            } else {
                Toast.makeText(requireContext(), "حدث خطأ أثناء حفظ الأوردر", Toast.LENGTH_SHORT).show()
                view?.findViewById<Button>(R.id.btnSave)?.isEnabled = true
            }
        }
    }
}
