package com.example.orderreminders.data

import com.google.firebase.firestore.PropertyName

data class OrderEntity(
    var id: String = "",
    var orderNumber: Int = 0,
    var orderType: String = "",
    var customerCode: String = "",
    var phone: String = "",
    var priority: String = "",
    var reminderTime: Long? = null,
    var notes: String = "",
    
    @get:PropertyName("isCompleted")
    @set:PropertyName("isCompleted")
    var isCompleted: Boolean = false,

    @get:PropertyName("isContacted")
    @set:PropertyName("isContacted")
    var isContacted: Boolean = false,
    
    var notificationSent: Boolean = false,
    var createdAt: Long = System.currentTimeMillis(),
    var items: List<OrderItemEntity> = emptyList()
)
