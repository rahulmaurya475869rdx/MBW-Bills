package com.mbwbill.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object ImageUtils {
    suspend fun compressAndSave(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        try {
            val input = context.contentResolver.openInputStream(uri) ?: return@withContext ""
            val bitmap = BitmapFactory.decodeStream(input)
            input.close()
            if (bitmap == null) return@withContext ""

            val scaled = if (bitmap.width > 1280) {
                val ratio = 1280f / bitmap.width
                Bitmap.createScaledBitmap(bitmap, 1280, (bitmap.height * ratio).toInt(), true)
            } else bitmap

            val fileName = "img_${System.currentTimeMillis()}.jpg"
            val imagesFolder = StorageManager.getImagesFolder(context)
            
            if (imagesFolder != null) {
                val file = imagesFolder.createFile("image/jpeg", fileName)
                if (file != null) {
                    val outputStream = context.contentResolver.openOutputStream(file.uri)
                    if (outputStream != null) {
                        scaled.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
                        outputStream.close()
                        return@withContext file.uri.toString()
                    }
                }
            }
            
            // Fallback agar user ne folder select nahi kiya
            val fallbackDir = File(context.filesDir, "images").apply { mkdirs() }
            val fallbackFile = File(fallbackDir, fileName)
            FileOutputStream(fallbackFile).use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 75, out)
            }
            fallbackFile.absolutePath
            
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
}
