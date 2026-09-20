package com.example.orderreminders

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.orderreminders.adapter.OrderAdapter
import com.example.orderreminders.data.OrderEntity
import com.example.orderreminders.data.OrderItemEntity
import com.example.orderreminders.utils.BrandingHelper
import com.example.orderreminders.viewmodel.OrderViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class OrdersFragment : Fragment() {

    private lateinit var viewModel: OrderViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: OrderAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_orders, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        BrandingHelper.applyBranding(requireContext(), view)


        viewModel = ViewModelProvider(requireActivity())[OrderViewModel::class.java]
        recyclerView = view.findViewById(R.id.recyclerOrders)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = OrderAdapter(
            onDoneClick = { order ->
                if (order.isCompleted) {
                    viewModel.undoDone(order)
                } else {
                    viewModel.markDone(order)
                }
            },
            onDeleteClick = { order ->
                viewModel.deleteOrder(order)
            },
            onEditClick = { order ->
                showEditOrderDialog(order)
            },
            onContactedChange = { order, isContacted ->
                viewModel.markContacted(order, isContacted)
            }
        )
        recyclerView.adapter = adapter

        val tvEmptyState = view.findViewById<TextView>(R.id.tvEmptyState)
        
        viewModel.openOrders.asLiveData().observe(viewLifecycleOwner) { orders ->
            adapter.submitList(orders)
            if (orders.isEmpty()) {
                recyclerView.visibility = View.GONE
                tvEmptyState.visibility = View.VISIBLE
            } else {
                recyclerView.visibility = View.VISIBLE
                tvEmptyState.visibility = View.GONE
            }
        }
    }

    private fun showEditOrderDialog(order: OrderEntity) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_order, null)
        val etCustomerCode = dialogView.findViewById<EditText>(R.id.etEditCustomerCode)
        val etPhone = dialogView.findViewById<EditText>(R.id.etEditPhone)
        val etEditNotes = dialogView.findViewById<EditText>(R.id.etEditNotes)
        
        val toggleEditOrderType = dialogView.findViewById<MaterialButtonToggleGroup>(R.id.toggleEditOrderType)
        val toggleEditPriorityGroup = dialogView.findViewById<MaterialButtonToggleGroup>(R.id.toggleEditPriorityGroup)

        val btnEditDate = dialogView.findViewById<MaterialButton>(R.id.btnEditDate)
        val tvEditDate = dialogView.findViewById<TextView>(R.id.tvEditDate)
        val btnEditTime = dialogView.findViewById<MaterialButton>(R.id.btnEditTime)
        val tvEditTime = dialogView.findViewById<TextView>(R.id.tvEditTime)

        val itemsContainer = dialogView.findViewById<LinearLayout>(R.id.editItemsContainer)
        val btnSave = dialogView.findViewById<Button>(R.id.btnEditSave)

        // Pre-fill text fields
        etCustomerCode.setText(order.customerCode)
        etPhone.setText(order.phone)
        etEditNotes.setText(order.notes)

        // Pre-fill Type
        if (order.orderType == "DELIVERY") {
            toggleEditOrderType.check(R.id.btnEditDelivery)
        } else {
            toggleEditOrderType.check(R.id.btnEditPickup)
        }

        // Pre-fill Priority
        when (order.priority) {
            "URGENT" -> toggleEditPriorityGroup.check(R.id.btnEditPriorityUrgent)
            "SHORTAGE" -> toggleEditPriorityGroup.check(R.id.btnEditPriorityShortage)
            else -> toggleEditPriorityGroup.check(R.id.btnEditPriorityNormal)
        }

        val editDialogRootCard = dialogView.findViewById<MaterialCardView>(R.id.editDialogRootCard)
        val tvEditTitle = dialogView.findViewById<TextView>(R.id.tvEditTitle)
        val tvEditTypeLabel = dialogView.findViewById<TextView>(R.id.tvEditTypeLabel)
        val tvEditPriorityLabel = dialogView.findViewById<TextView>(R.id.tvEditPriorityLabel)
        val tvEditTimeLabel = dialogView.findViewById<TextView>(R.id.tvEditTimeLabel)
        val tvEditItemsLabel = dialogView.findViewById<TextView>(R.id.tvEditItemsLabel)
        val btnEditAddItem = dialogView.findViewById<Button>(R.id.btnEditAddItem)

        val tilEditCustomerCode = dialogView.findViewById<TextInputLayout>(R.id.tilEditCustomerCode)
        val tilEditPhone = dialogView.findViewById<TextInputLayout>(R.id.tilEditPhone)
        val tilEditNotes = dialogView.findViewById<TextInputLayout>(R.id.tilEditNotes)

        val btnEditDelivery = dialogView.findViewById<MaterialButton>(R.id.btnEditDelivery)
        val btnEditPickup = dialogView.findViewById<MaterialButton>(R.id.btnEditPickup)
        val btnEditPriorityNormal = dialogView.findViewById<MaterialButton>(R.id.btnEditPriorityNormal)
        val btnEditPriorityUrgent = dialogView.findViewById<MaterialButton>(R.id.btnEditPriorityUrgent)
        val btnEditPriorityShortage = dialogView.findViewById<MaterialButton>(R.id.btnEditPriorityShortage)

        val initialColor = when (order.priority) {
            "URGENT" -> Color.parseColor("#E53935") // Red
            "SHORTAGE" -> Color.parseColor("#F9A825") // Yellow
            else -> ContextCompat.getColor(requireContext(), R.color.brand_cyan)
        }

        var currentThemeColor = initialColor

        fun findAllViewsOfType(viewGroup: ViewGroup, clazz: Class<*>): List<View> {
            val result = mutableListOf<View>()
            for (i in 0 until viewGroup.childCount) {
                val child = viewGroup.getChildAt(i)
                if (clazz.isInstance(child)) {
                    result.add(child)
                }
                if (child is ViewGroup) {
                    result.addAll(findAllViewsOfType(child, clazz))
                }
            }
            return result
        }

        fun applyToTextInputLayout(til: TextInputLayout, themeColor: Int) {
            val states = arrayOf(
                intArrayOf(android.R.attr.state_focused),
                intArrayOf(android.R.attr.state_hovered),
                intArrayOf(android.R.attr.state_enabled),
                intArrayOf()
            )
            val colors = intArrayOf(
                themeColor,
                themeColor,
                themeColor,
                themeColor
            )
            val csl = ColorStateList(states, colors)
            til.boxStrokeColor = themeColor
            til.setBoxStrokeColor(themeColor)
            til.setBoxStrokeColorStateList(csl)
            til.hintTextColor = csl
            til.defaultHintTextColor = csl
            til.editText?.setTextColor(themeColor)
        }

        fun applyThemeToItemView(itemView: View, color: Int, bgColor: Int) {
            val csl = ColorStateList.valueOf(color)
            (itemView as? MaterialCardView)?.let { card ->
                card.strokeColor = color
                card.setCardBackgroundColor(bgColor)
            }
            findAllViewsOfType(itemView as ViewGroup, TextView::class.java).forEach { tv ->
                if (tv.id != R.id.btnDeleteItem) {
                    (tv as TextView).setTextColor(color)
                }
            }
            findAllViewsOfType(itemView, TextInputLayout::class.java).forEach { til ->
                applyToTextInputLayout(til as TextInputLayout, color)
                (til as TextInputLayout).setEndIconTintList(csl)
            }
            findAllViewsOfType(itemView, MaterialAutoCompleteTextView::class.java).forEach { tv ->
                (tv as TextView).setTextColor(color)
            }
            findAllViewsOfType(itemView, EditText::class.java).forEach { et ->
                (et as TextView).setTextColor(color)
            }
        }

        fun applyTheme(color: Int) {
            currentThemeColor = color

            val theme = requireContext().theme
            val typedValue = TypedValue()
            theme.resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true)
            val surfaceColor = typedValue.data

            val currentPriority = when (toggleEditPriorityGroup.checkedButtonId) {
                R.id.btnEditPriorityUrgent -> "URGENT"
                R.id.btnEditPriorityShortage -> "SHORTAGE"
                else -> "NORMAL"
            }
            val dialogBgColor = when (currentPriority) {
                "URGENT" -> Color.parseColor("#FFEBEE") // Light red
                "SHORTAGE" -> Color.parseColor("#FFFDE7") // Light yellow
                else -> surfaceColor
            }

            val shapeAppearanceModel = ShapeAppearanceModel.builder()
                .setAllCorners(CornerFamily.ROUNDED, 16f * resources.displayMetrics.density)
                .build()

            val shapeDrawable = MaterialShapeDrawable(shapeAppearanceModel).apply {
                fillColor = ColorStateList.valueOf(dialogBgColor)
                strokeColor = ColorStateList.valueOf(color)
                strokeWidth = 2.5f * resources.displayMetrics.density
            }
            editDialogRootCard.background = shapeDrawable
            editDialogRootCard.strokeWidth = 0

            tvEditTitle.setTextColor(color)
            tvEditTypeLabel.setTextColor(color)
            tvEditPriorityLabel.setTextColor(color)
            tvEditTimeLabel.setTextColor(color)
            tvEditItemsLabel.setTextColor(color)
            btnEditAddItem.setTextColor(color)

            applyToTextInputLayout(tilEditCustomerCode, color)
            applyToTextInputLayout(tilEditPhone, color)
            applyToTextInputLayout(tilEditNotes, color)

            val colorStateList = ColorStateList.valueOf(color)

            btnEditDate.strokeColor = colorStateList
            btnEditTime.strokeColor = colorStateList
            btnEditDate.setTextColor(color)
            btnEditTime.setTextColor(color)
            tvEditDate.setTextColor(color)
            tvEditTime.setTextColor(color)

            val checkedState = intArrayOf(android.R.attr.state_checked)
            val uncheckedState = intArrayOf(-android.R.attr.state_checked)

            val toggleBgColorStateList = ColorStateList(
                arrayOf(checkedState, uncheckedState),
                intArrayOf(color, Color.TRANSPARENT)
            )

            val toggleTextColorStateList = ColorStateList(
                arrayOf(checkedState, uncheckedState),
                intArrayOf(Color.WHITE, color)
            )

            val toggleButtons = listOf(
                btnEditDelivery, btnEditPickup,
                btnEditPriorityNormal, btnEditPriorityUrgent, btnEditPriorityShortage
            )
            toggleButtons.forEach { btn ->
                btn.backgroundTintList = toggleBgColorStateList
                btn.setTextColor(toggleTextColorStateList)
                btn.strokeColor = colorStateList
            }

            btnSave.backgroundTintList = colorStateList

            for (i in 0 until itemsContainer.childCount) {
                applyThemeToItemView(itemsContainer.getChildAt(i), color, dialogBgColor)
            }
        }

        applyTheme(initialColor)

        toggleEditPriorityGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val newColor = when (checkedId) {
                    R.id.btnEditPriorityUrgent -> Color.parseColor("#E53935")
                    R.id.btnEditPriorityShortage -> Color.parseColor("#F9A825")
                    else -> ContextCompat.getColor(requireContext(), R.color.brand_cyan)
                }
                applyTheme(newColor)
            }
        }

        // Setup Date/Time
        val selectedCalendar = Calendar.getInstance()
        if (order.reminderTime != null && order.reminderTime!! > 0L) {
            selectedCalendar.timeInMillis = order.reminderTime!!
        }

        fun updateDateTimeViews() {
            val sdfDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val sdfTime = SimpleDateFormat("hh:mm a", Locale.getDefault())
            tvEditDate.text = sdfDate.format(selectedCalendar.time)
            tvEditTime.text = sdfTime.format(selectedCalendar.time)
        }
        updateDateTimeViews()

        btnEditDate.setOnClickListener {
            DatePickerDialog(requireContext(), { _, year, month, dayOfMonth ->
                selectedCalendar.set(Calendar.YEAR, year)
                selectedCalendar.set(Calendar.MONTH, month)
                selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                updateDateTimeViews()
            }, selectedCalendar.get(Calendar.YEAR), selectedCalendar.get(Calendar.MONTH), selectedCalendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        btnEditTime.setOnClickListener {
            TimePickerDialog(requireContext(), { _, hourOfDay, minute ->
                selectedCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                selectedCalendar.set(Calendar.MINUTE, minute)
                updateDateTimeViews()
            }, selectedCalendar.get(Calendar.HOUR_OF_DAY), selectedCalendar.get(Calendar.MINUTE), false).show()
        }

        // Pre-fill Items
        val statusOptions = arrayOf("متوفرة", "سيتم الطلب من المخزن", "تم الطلب من المخزن")
        val statusMapToInternal = mapOf(
            "متوفرة" to "AVAILABLE",
            "متوفر" to "AVAILABLE",
            "سيتم الطلب من المخزن" to "TO_BE_ORDERED",
            "تم الطلب من المخزن" to "ORDERED",
            "استلام الطلب" to "RECEIVED"
        )
        val statusMapToDisplay = mapOf(
            "AVAILABLE" to "متوفرة",
            "TO_BE_ORDERED" to "سيتم الطلب من المخزن",
            "ORDERED" to "تم الطلب من المخزن",
            "ORDERED_FROM_WAREHOUSE" to "تم الطلب من المخزن",
            "RECEIVED" to "استلام الطلب"
        )

        fun addEditItemView(name: String = "", availability: String = "AVAILABLE", warehouseName: String = "") {
            val itemView = LayoutInflater.from(requireContext()).inflate(R.layout.item_order_input, itemsContainer, false)

            val currentPriority = when (toggleEditPriorityGroup.checkedButtonId) {
                R.id.btnEditPriorityUrgent -> "URGENT"
                R.id.btnEditPriorityShortage -> "SHORTAGE"
                else -> "NORMAL"
            }
            val theme = requireContext().theme
            val typedValue = TypedValue()
            theme.resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true)
            val surfaceColor = typedValue.data
            val currentBgColor = when (currentPriority) {
                "URGENT" -> Color.parseColor("#FFEBEE")
                "SHORTAGE" -> Color.parseColor("#FFFDE7")
                else -> surfaceColor
            }

            applyThemeToItemView(itemView, currentThemeColor, currentBgColor)

            val etItemName = itemView.findViewById<EditText>(R.id.etItemName)
            val spinnerAvailability = itemView.findViewById<MaterialAutoCompleteTextView>(R.id.spinnerAvailability)
            val tilWarehouse = itemView.findViewById<TextInputLayout>(R.id.tilWarehouse)
            val etWarehouse = itemView.findViewById<EditText>(R.id.etWarehouse)
            val btnDeleteItem = itemView.findViewById<Button>(R.id.btnDeleteItem)

            etItemName.setText(name)

            val displayStatus = statusMapToDisplay[availability] ?: "متوفرة"
            spinnerAvailability.setText(displayStatus, false)
            spinnerAvailability.setTextColor(currentThemeColor)

            val adapter = object : ArrayAdapter<String>(requireContext(), R.layout.item_dropdown_status, statusOptions) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = super.getView(position, convertView, parent)
                    (view as? TextView)?.setTextColor(currentThemeColor)
                    return view
                }

                override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = super.getDropDownView(position, convertView, parent)
                    (view as? TextView)?.setTextColor(currentThemeColor)
                    return view
                }
            }
            spinnerAvailability.setAdapter(adapter)

            if (availability == "ORDERED" || availability == "ORDERED_FROM_WAREHOUSE") {
                tilWarehouse.visibility = View.VISIBLE
                etWarehouse.setText(warehouseName)
            } else {
                tilWarehouse.visibility = View.GONE
                etWarehouse.setText("")
            }

            spinnerAvailability.setOnItemClickListener { _, _, position, _ ->
                if (statusOptions[position] == "تم الطلب من المخزن") {
                    tilWarehouse.visibility = View.VISIBLE
                } else {
                    tilWarehouse.visibility = View.GONE
                    etWarehouse.setText("")
                }
            }

            btnDeleteItem.setOnClickListener {
                itemsContainer.removeView(itemView)
            }

            itemsContainer.addView(itemView)
        }

        order.items.forEach { item ->
            addEditItemView(item.name, item.availability, item.warehouseName)
        }

        btnEditAddItem.setOnClickListener {
            addEditItemView()
        }

        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.Theme_OrderReminders_Dialog)
            .setView(dialogView)
            .show()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val displayMetrics = resources.displayMetrics
        val dialogWidth = (displayMetrics.widthPixels * 0.92).toInt()
        val dialogHeight = (displayMetrics.heightPixels * 0.6).toInt()
        dialog.window?.setLayout(dialogWidth, dialogHeight)

        btnSave.setOnClickListener {
            order.customerCode = etCustomerCode.text.toString()
            order.phone = etPhone.text.toString()
            order.notes = etEditNotes.text.toString().trim()

            order.orderType = if (toggleEditOrderType.checkedButtonId == R.id.btnEditDelivery) "DELIVERY" else "PICKUP"
            order.priority = when (toggleEditPriorityGroup.checkedButtonId) {
                R.id.btnEditPriorityUrgent -> "URGENT"
                R.id.btnEditPriorityShortage -> "SHORTAGE"
                else -> "NORMAL"
            }
            order.reminderTime = selectedCalendar.timeInMillis

            val updatedItems = mutableListOf<OrderItemEntity>()
            for (i in 0 until itemsContainer.childCount) {
                val itemView = itemsContainer.getChildAt(i)
                val etItemName = itemView.findViewById<EditText>(R.id.etItemName)
                val spinnerAvailability = itemView.findViewById<MaterialAutoCompleteTextView>(R.id.spinnerAvailability)
                val etWarehouse = itemView.findViewById<EditText>(R.id.etWarehouse)

                if (etItemName != null && spinnerAvailability != null) {
                    val name = etItemName.text.toString().trim()
                    val availabilityStr = spinnerAvailability.text.toString()
                    val availability = statusMapToInternal[availabilityStr] ?: "AVAILABLE"
                    val warehouseName = etWarehouse?.text?.toString()?.trim() ?: ""

                    if (name.isNotEmpty()) {
                        val finalWarehouse = if (availability == "ORDERED") warehouseName else ""
                        val existingId = if (i < order.items.size) order.items[i].id else ""
                        updatedItems.add(
                            OrderItemEntity(
                                id = existingId,
                                orderId = order.id,
                                name = name,
                                availability = availability,
                                warehouseName = finalWarehouse
                            )
                        )
                    }
                }
            }
            order.items = updatedItems

            viewModel.updateOrder(order)
            dialog.dismiss()
        }
    }
}
