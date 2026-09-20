package com.example.orderreminders.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.orderreminders.data.OrderEntity
import com.example.orderreminders.data.OrderItemEntity
import com.example.orderreminders.repository.OrderRepository
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class OrderViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = OrderRepository(application.applicationContext)

    // Using Flow map to cleanly separate Open vs Completed orders
    // without risking Firebase missing-field index crashes
    val openOrders = repository.getAllOrders().map { orders ->
        orders.filter { !it.isCompleted }
    }

    val completedOrders = repository.getAllOrders().map { orders ->
        orders.filter { it.isCompleted }
    }

    suspend fun addOrder(
        type: String,
        customerCode: String,
        phone: String,
        priority: String,
        reminderTime: Long?,
        items: List<OrderItemEntity>,
        notes: String = ""
    ): Boolean {
        val order = OrderEntity(
            orderType = type,
            customerCode = customerCode,
            phone = phone,
            priority = priority,
            reminderTime = reminderTime,
            isCompleted = false,
            notes = notes
        )

        return repository.insertOrder(order, items)
    }

    fun markDone(order: OrderEntity) {
        viewModelScope.launch {
            repository.markAsCompleted(order.id, true)
        }
    }

    fun markContacted(order: OrderEntity, isContacted: Boolean) {
        viewModelScope.launch {
            repository.markContacted(order.id, isContacted)
        }
    }

    fun updateOrder(order: OrderEntity) {
        viewModelScope.launch {
            repository.updateOrder(order)
        }
    }

    fun undoDone(order: OrderEntity) {
        viewModelScope.launch {
            repository.markAsCompleted(order.id, false)
        }
    }

    fun deleteOrder(order: OrderEntity) {
        viewModelScope.launch {
            repository.deleteOrder(order)
        }
    }
}
