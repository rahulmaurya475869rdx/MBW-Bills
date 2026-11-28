package com.mbwbill.app.util

import android.content.Context
import android.net.Uri
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

object CloudinaryUploader {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    fun upload(context: Context, uriString: String): String? {
        return try {
            val uri = Uri.parse(uriString)
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            
            val tempFile = File(context.cacheDir, "upload_${System.currentTimeMillis()}.jpg")
            tempFile.outputStream().use { output ->
                inputStream.copyTo(output)
            }
            inputStream.close()

            val body = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("upload_preset", Config.CLOUDINARY_UPLOAD_PRESET)
                .addFormDataPart("file", tempFile.name, tempFile.asRequestBody("image/*".toMediaType()))
                .build()
                
            val req = Request.Builder()
                .url("https://api.cloudinary.com/v1_1/${Config.CLOUDINARY_CLOUD_NAME}/image/upload")
                .post(body)
                .build()
                
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    tempFile.delete()
                    return null
                }
                val json = JSONObject(resp.body?.string() ?: "")
                tempFile.delete()
                json.optString("secure_url").ifBlank { null }
            }
        } catch (e: Exception) { 
            e.printStackTrace()
            null 
        }
    }
}
