package com.example.orderreminders.data

data class OrderItemEntity(
    var id: String = "",
    var orderId: String = "",
    var name: String = "",
    var availability: String = "",
    var warehouseName: String = ""
)
