package com.example.orderreminders.repository

import android.content.Context
import android.util.Log
import com.example.orderreminders.data.OrderEntity
import com.example.orderreminders.data.OrderItemEntity
import com.example.orderreminders.utils.NotificationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class OrderRepository(private val context: Context? = null) {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val currentUserId: String
        get() = auth.currentUser?.uid ?: "anonymous"

    private val ordersCollection
        get() = db.collection("Users").document(currentUserId).collection("Orders")

    fun getAllOrders(): Flow<List<OrderEntity>> = callbackFlow {
        var isInitialLoad = true
        
        val listener = ordersCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("OrderRepository", "Listen failed.", error)
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                // Check for new additions to fire notifications (ignore initial load and local writes)
                if (!isInitialLoad && context != null) {
                    for (dc in snapshot.documentChanges) {
                        if (dc.type == DocumentChange.Type.ADDED) {
                            // Local writes are covered directly in the Activity to avoid duplicates/misses
                            if (!dc.document.metadata.hasPendingWrites()) {
                                val order = dc.document.toObject(OrderEntity::class.java)
                                val firstItemName = order.items.firstOrNull()?.name ?: "عنصر غير معروف"
                                NotificationHelper.showNewOrderNotification(
                                    context,
                                    order.customerCode,
                                    firstItemName
                                )
                            }
                        }
                    }
                }
                
                isInitialLoad = false

                val orders = snapshot.documents.mapNotNull { doc ->
                    val order = doc.toObject(OrderEntity::class.java)
                    order?.id = doc.id
                    order
                }.sortedWith(
                    compareBy { it.reminderTime ?: Long.MAX_VALUE }
                )
                trySend(orders).isSuccess
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun insertOrder(order: OrderEntity, items: List<OrderItemEntity>): Boolean {
        return try {
            val documentRef = ordersCollection.document()
            order.id = documentRef.id
            order.items = items
            
            val snapshot = ordersCollection.get().await()
            val maxNumber = snapshot.documents.mapNotNull { it.getLong("orderNumber") }.maxOrNull() ?: 0L
            order.orderNumber = (maxNumber + 1).toInt()

            documentRef.set(order).await()
            true
        } catch (e: Exception) {
            Log.e("OrderRepository", "Error inserting order", e)
            false
        }
    }

    suspend fun updateOrder(order: OrderEntity) {
        try {
            ordersCollection.document(order.id).set(order).await()
        } catch (e: Exception) {
            Log.e("OrderRepository", "Error updating order", e)
        }
    }

    suspend fun deleteOrder(order: OrderEntity) {
        try {
            ordersCollection.document(order.id).delete().await()
        } catch (e: Exception) {
            Log.e("OrderRepository", "Error deleting order", e)
        }
    }

    suspend fun markAsCompleted(orderId: String, isCompleted: Boolean) {
        try {
            ordersCollection.document(orderId).update("isCompleted", isCompleted).await()
        } catch (e: Exception) {
            Log.e("OrderRepository", "Error marking completed", e)
        }
    }

    suspend fun markContacted(orderId: String, isContacted: Boolean) {
        try {
            ordersCollection.document(orderId).update("isContacted", isContacted).await()
        } catch (e: Exception) {
            Log.e("OrderRepository", "Error marking contacted", e)
        }
    }

    suspend fun getOrdersThatNeedNotification(currentTime: Long): List<OrderEntity> {
        return try {
            val snapshot = ordersCollection
                .whereEqualTo("notificationSent", false)
                .whereLessThanOrEqualTo("reminderTime", currentTime)
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                val order = doc.toObject(OrderEntity::class.java)
                order?.id = doc.id
                order
            }.filter { !it.isCompleted } // Filter out completed orders client-side
        } catch (e: Exception) {
            Log.e("OrderRepository", "Error fetching notifications", e)
            emptyList()
        }
    }

    suspend fun markNotificationSent(orderId: String) {
        try {
            ordersCollection.document(orderId).update("notificationSent", true).await()
        } catch (e: Exception) {
            Log.e("OrderRepository", "Error marking notification sent", e)
        }
    }

    suspend fun migrateLegacyDataIfNecessary(pharmacyName: String) {
        if (pharmacyName.trim() != "يسر") return

        val currentUid = auth.currentUser?.uid ?: return

        try {
            val collectionsToMigrate = listOf("orders", "Orders")
            val targetCollection = db.collection("Users").document(currentUid).collection("Orders")

            for (legacyColName in collectionsToMigrate) {
                val legacyRef = db.collection(legacyColName)
                val snapshot = legacyRef.get().await()

                if (!snapshot.isEmpty) {
                    val writeBatch = db.batch()
                    for (doc in snapshot.documents) {
                        val data = doc.data ?: continue
                        val newDocRef = targetCollection.document(doc.id)
                        writeBatch.set(newDocRef, data)
                    }
                    writeBatch.commit().await()

                    // Clear old root collection
                    val deleteBatch = db.batch()
                    for (doc in snapshot.documents) {
                        deleteBatch.delete(doc.reference)
                    }
                    deleteBatch.commit().await()

                    Log.d("OrderRepository", "Successfully migrated legacy collection '$legacyColName' for pharmacy 'يسر'")
                }
            }
        } catch (e: Exception) {
            Log.e("OrderRepository", "Error during legacy migration for 'يسر'", e)
        }
    }
}
