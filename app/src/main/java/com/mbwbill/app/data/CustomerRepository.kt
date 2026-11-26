package com.mbwbill.app.data

class CustomerRepository(private val dao: CustomerDao) {
    fun getAll() = dao.getAll()
    fun search(q: String) = if (q.isBlank()) dao.getAll() else dao.search(q)
    suspend fun getById(id: Long) = dao.getById(id)
    suspend fun insert(c: Customer) = dao.insert(c)
    suspend fun update(c: Customer) = dao.update(c)
    suspend fun deleteById(id: Long) = dao.deleteById(id)
    suspend fun getUnsynced() = dao.getUnsynced()
    suspend fun markSynced(id: Long) = dao.markSynced(id)
}
