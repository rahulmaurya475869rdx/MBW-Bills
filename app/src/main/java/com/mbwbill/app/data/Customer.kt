package com.mbwbill.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val address: String,
    val mobile: String,
    val serialNo: String,
    val dateOfSale: String,
    val dateOfInvoice: String = "",
    val billImages: String = "[]",
    val warrantyImages: String = "[]",
    val synced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
