package com.mbwbill.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY createdAt DESC")
    fun getAll(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE name LIKE '%' || :q || '%' OR mobile LIKE '%' || :q || '%' OR serialNo LIKE '%' || :q || '%' ORDER BY createdAt DESC")
    fun search(q: String): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getById(id: Long): Customer?

    @Query("SELECT * FROM customers WHERE synced = 0")
    suspend fun getUnsynced(): List<Customer>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(c: Customer): Long

    @Update
    suspend fun update(c: Customer)

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE customers SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: Long)
}
