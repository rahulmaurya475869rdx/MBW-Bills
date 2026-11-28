package com.mbwbill.app.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.mbwbill.app.data.AppDatabase
import com.mbwbill.app.util.CloudinaryUploader
import kotlinx.coroutines.tasks.await

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            val dao = AppDatabase.get(applicationContext).customerDao()
            val unsynced = dao.getUnsynced()
            val db = FirebaseFirestore.getInstance()
            
            for (c in unsynced) {
                val billUrls = uploadImages(c.billImages)
                val warrantyUrls = uploadImages(c.warrantyImages)
                
                // Firestore document ID me createdAt use kiya taaki reinstall pe overwrite na ho
                val data = hashMapOf(
                    "name" to c.name,
                    "address" to c.address,
                    "mobile" to c.mobile,
                    "serialNo" to c.serialNo,
                    "dateOfSale" to c.dateOfSale,
                    "dateOfInvoice" to c.dateOfInvoice,
                    "billImages" to billUrls,
                    "warrantyImages" to warrantyUrls,
                    "createdAt" to c.createdAt
                )
                
                db.collection("customers").document(c.createdAt.toString()).set(data).await()
                dao.markSynced(c.id)
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun uploadImages(json: String): List<String> {
        if (json.isBlank() || json == "[]") return emptyList()
        return try {
            val paths: Array<String> = Gson().fromJson(json, Array<String>::class.java)
            paths.mapNotNull { p ->
                CloudinaryUploader.upload(applicationContext, p)
            }
        } catch (e: Exception) { emptyList() }
    }
}
