package com.example.orderreminders.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.graphics.drawable.RippleDrawable
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.orderreminders.R
import com.example.orderreminders.data.OrderEntity
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OrderAdapter(
    private val onDoneClick: (OrderEntity) -> Unit,
    private val onDeleteClick: (OrderEntity) -> Unit,
    private val onEditClick: ((OrderEntity) -> Unit)? = null,
    private val onContactedChange: ((OrderEntity, Boolean) -> Unit)? = null
) : ListAdapter<OrderEntity, OrderAdapter.OrderViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_order, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val tvOrderNumber = itemView.findViewById<TextView>(R.id.tvOrderNumber)
        private val tvCustomer = itemView.findViewById<TextView>(R.id.tvCustomer)
        private val tvPriority = itemView.findViewById<TextView>(R.id.tvPriority)
        private val ivTypeIcon = itemView.findViewById<ImageView>(R.id.ivTypeIcon)

        fun bind(order: OrderEntity) {

            tvOrderNumber.text = "أوردر ${order.orderNumber}"
            tvCustomer.text = "كود العميل: ${order.customerCode}"

            if (order.orderType == "DELIVERY") {
                ivTypeIcon.setImageResource(R.drawable.shipping)
            } else {
                ivTypeIcon.setImageResource(R.drawable.hand_package)
            }

            // Priority Styling
            val cardView = itemView as MaterialCardView
            val theme = itemView.context.theme
            val typedValue = TypedValue()
            theme.resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true)
            val surfaceColor = typedValue.data
            
            cardView.setCardBackgroundColor(surfaceColor) // Use dynamic surface color

            val dp2 = (2 * itemView.context.resources.displayMetrics.density).toInt()
            val primaryColor = ContextCompat.getColor(itemView.context, R.color.brand_cyan)

            when {
                order.isCompleted -> {
                    val archiveColor = Color.parseColor("#4CAF50") // Material Green
                    tvPriority?.text = "مؤرشف"
                    tvPriority?.background?.setTint(archiveColor)
                    cardView.strokeColor = archiveColor
                    cardView.setCardBackgroundColor(Color.parseColor("#E8F5E9")) // Very Light Green Background
                    cardView.strokeWidth = dp2
                    
                    tvCustomer?.setTextColor(archiveColor)
                    tvOrderNumber?.setTextColor(archiveColor)
                    ivTypeIcon?.setColorFilter(archiveColor, PorterDuff.Mode.SRC_IN)
                }
                order.priority == "URGENT" -> {
                    val urgentColor = Color.parseColor("#E53935") // Red
                    tvPriority?.text = "مستعجل"
                    tvPriority?.background?.setTint(Color.parseColor("#EF5350")) // Crimson Chip
                    cardView.strokeColor = Color.parseColor("#EF5350")
                    cardView.setCardBackgroundColor(Color.parseColor("#FFEBEE")) // Light Red Background
                    cardView.strokeWidth = dp2
                    
                    tvCustomer?.setTextColor(urgentColor)
                    tvOrderNumber?.setTextColor(urgentColor)
                    ivTypeIcon?.setColorFilter(urgentColor, PorterDuff.Mode.SRC_IN)
                }
                order.priority == "SHORTAGE" -> {
                    val shortageColor = Color.parseColor("#F9A825") // Strong Golden Yellow
                    tvPriority?.text = "نواقص"
                    tvPriority?.background?.setTint(Color.parseColor("#FFC107")) // Yellow Chip
                    cardView.strokeColor = Color.parseColor("#FFC107") // Yellow stroke
                    cardView.setCardBackgroundColor(Color.parseColor("#FFFDE7")) // Light Yellow Background
                    cardView.strokeWidth = dp2
                    
                    tvCustomer?.setTextColor(shortageColor)
                    tvOrderNumber?.setTextColor(shortageColor)
                    ivTypeIcon?.setColorFilter(shortageColor, PorterDuff.Mode.SRC_IN)
                }
                else -> {
                    tvPriority?.text = "عادي"
                    tvPriority?.background?.setTint(primaryColor) // Brand Cyan Chip
                    cardView.strokeColor = primaryColor // Brand Cyan stroke
                    cardView.setCardBackgroundColor(surfaceColor) // Dynamic Surface Background
                    cardView.strokeWidth = dp2 // Match width
                    
                    tvCustomer?.setTextColor(primaryColor)
                    tvOrderNumber?.setTextColor(primaryColor)
                    ivTypeIcon?.setColorFilter(primaryColor, PorterDuff.Mode.SRC_IN)
                }
            }

            // Click listener for details dialog
            itemView.setOnClickListener {
                showOrderDetailsDialog(order, it)
            }
        }

        private fun showOrderDetailsDialog(order: OrderEntity, view: View) {
            val context = view.context
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_order_details, null)

            val title = dialogView.findViewById<TextView>(R.id.dialogTitle)
            val details = dialogView.findViewById<TextView>(R.id.dialogDetails)
            val cbContacted = dialogView.findViewById<CheckBox>(R.id.cbContacted)
            val itemsContainer = dialogView.findViewById<LinearLayout>(R.id.dialogItemsContainer)
            val btnDone = dialogView.findViewById<ImageButton>(R.id.btnDialogDone)
            val btnDelete = dialogView.findViewById<ImageButton>(R.id.btnDialogDelete)
            val btnEdit = dialogView.findViewById<ImageButton>(R.id.btnDialogEdit)
            val btnClose = dialogView.findViewById<ImageButton>(R.id.btnDialogClose)
            val dialogRootCard = dialogView.findViewById<MaterialCardView>(R.id.dialogRootCard)

            val primaryColor = ContextCompat.getColor(context, R.color.brand_cyan)

            // Populate data (Clean customer code & order number display without hyphens/dashes)
            title.text = "أوردر ${order.orderNumber}"

            val time = order.reminderTime
            val typeStr = if (order.orderType == "DELIVERY") "توصيل" else "استلام"
            
            val theme = context.theme
            val typedValue = TypedValue()
            theme.resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true)
            val surfaceColor = typedValue.data

            val contentColor: Int
            val priorityStr = when {
                order.isCompleted -> {
                    dialogRootCard.strokeColor = Color.parseColor("#4CAF50") // Green
                    dialogRootCard.setCardBackgroundColor(Color.parseColor("#E8F5E9")) // Light Green
                    contentColor = Color.parseColor("#4CAF50")
                    "مؤرشف"
                }
                order.priority == "URGENT" -> {
                    dialogRootCard.strokeColor = Color.parseColor("#EF5350")
                    dialogRootCard.setCardBackgroundColor(Color.parseColor("#FFEBEE"))
                    contentColor = Color.parseColor("#E53935")
                    "مستعجل"
                }
                order.priority == "SHORTAGE" -> {
                    dialogRootCard.strokeColor = Color.parseColor("#FFC107")
                    dialogRootCard.setCardBackgroundColor(Color.parseColor("#FFFDE7"))
                    contentColor = Color.parseColor("#F9A825") // Strong Golden Yellow
                    "نواقص"
                }
                else -> {
                    dialogRootCard.strokeColor = primaryColor
                    dialogRootCard.setCardBackgroundColor(surfaceColor)
                    contentColor = primaryColor
                    "عادي"
                }
            }
            
            title.setTextColor(contentColor)
            details.setTextColor(contentColor)
            cbContacted.setTextColor(contentColor)
            cbContacted.buttonTintList = ColorStateList.valueOf(contentColor)
            cbContacted.isChecked = order.isContacted
            cbContacted.setOnCheckedChangeListener { _, isChecked ->
                onContactedChange?.invoke(order, isChecked)
            }

            dialogView.findViewById<TextView>(R.id.dialogItemsLabel)?.setTextColor(contentColor)
            
            btnClose.setColorFilter(contentColor, PorterDuff.Mode.SRC_IN)
            btnEdit.setColorFilter(contentColor, PorterDuff.Mode.SRC_IN)
            btnDelete.setColorFilter(contentColor, PorterDuff.Mode.SRC_IN)
            btnDone.setColorFilter(contentColor, PorterDuff.Mode.SRC_IN)

            btnClose.background = RippleDrawable(ColorStateList.valueOf(contentColor), null, null)
            btnEdit.background = RippleDrawable(ColorStateList.valueOf(contentColor), null, null)
            btnDelete.background = RippleDrawable(ColorStateList.valueOf(contentColor), null, null)
            btnDone.background = RippleDrawable(ColorStateList.valueOf(contentColor), null, null)

            // Conditional Data Display (Hide empty optional fields)
            val detailsList = mutableListOf<String>()
            detailsList.add("كود العميل: ${order.customerCode}")
            if (order.phone.isNotBlank()) {
                detailsList.add("التليفون: ${order.phone}")
            }
            detailsList.add("النوع: $typeStr")
            detailsList.add("الأولوية: $priorityStr")
            if (time != null && time > 0L) {
                val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                detailsList.add("وقت التذكير: ${formatter.format(Date(time))}")
            }
            if (order.notes.isNotBlank()) {
                detailsList.add("الملاحظات: ${order.notes}")
            }
            details.text = detailsList.joinToString("\n")

            // Populate items (wrapped in clean card containers with status on a new line)
            itemsContainer.removeAllViews()
            order.items.forEach { item ->
                val itemCard = MaterialCardView(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 0, 0, 10)
                    }
                    radius = 12f * resources.displayMetrics.density
                    strokeWidth = (1 * resources.displayMetrics.density).toInt()
                    strokeColor = contentColor
                    setCardBackgroundColor(dialogRootCard.cardBackgroundColor.defaultColor)
                }

                val itemInnerLayout = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(16, 14, 16, 14)
                    layoutDirection = View.LAYOUT_DIRECTION_RTL
                }

                val tvName = TextView(context).apply {
                    text = "• ${item.name}"
                    textSize = 16f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(contentColor)
                }

                val avail = when (item.availability) {
                    "AVAILABLE" -> "متوفر"
                    "RECEIVED" -> "استلام الطلب"
                    "TO_BE_ORDERED" -> "سيتم الطلب من المخزن"
                    "ORDERED", "ORDERED_FROM_WAREHOUSE" -> "تم الطلب من المخزن"
                    else -> item.availability
                }
                val warehouseInfo = if (item.warehouseName.isNotBlank()) " - المخزن: ${item.warehouseName}" else ""

                val tvStatus = TextView(context).apply {
                    text = "الحالة: $avail$warehouseInfo"
                    textSize = 14f
                    setTextColor(contentColor)
                    setPadding(0, 4, 0, 0) // New line underneath item name
                }

                itemInnerLayout.addView(tvName)
                itemInnerLayout.addView(tvStatus)
                itemCard.addView(itemInnerLayout)
                itemsContainer.addView(itemCard)
            }
            
            if (order.isCompleted) {
                btnDone.setImageResource(R.drawable.unarchive)
                btnEdit.visibility = View.GONE
            } else {
                btnDone.setImageResource(R.drawable.task_alt)
                btnEdit.visibility = View.VISIBLE
            }

            // Show Dialog
            val dialog = MaterialAlertDialogBuilder(context, R.style.Theme_OrderReminders_Dialog)
                .setView(dialogView)
                .show()
                
            // Fix dialog background so the MaterialCardView border shows properly without double layering
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            btnClose.setOnClickListener {
                dialog.dismiss()
            }

            btnDone.setOnClickListener {
                onDoneClick(order)
                dialog.dismiss()
            }

            btnDelete.setOnClickListener {
                MaterialAlertDialogBuilder(context)
                    .setTitle("تأكيد الحذف")
                    .setMessage("هل أنت متأكد من حذف الطلب؟")
                    .setPositiveButton("نعم") { _, _ ->
                        onDeleteClick(order)
                        dialog.dismiss()
                    }
                    .setNegativeButton("لا", null)
                    .show()
            }

            btnEdit.setOnClickListener {
                dialog.dismiss()
                onEditClick?.invoke(order)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<OrderEntity>() {
        override fun areItemsTheSame(oldItem: OrderEntity, newItem: OrderEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: OrderEntity, newItem: OrderEntity): Boolean {
            return oldItem == newItem
        }
    }
}
