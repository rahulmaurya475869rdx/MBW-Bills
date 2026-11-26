package com.mbwbill.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object ImageUtils {
    fun compressAndSave(context: Context, uri: Uri): String {
        return try {
            val dir = File(context.filesDir, "images").apply { mkdirs() }
            val file = File(dir, "img_${System.currentTimeMillis()}.jpg")
            val input = context.contentResolver.openInputStream(uri) ?: return ""
            val bitmap = BitmapFactory.decodeStream(input)
            input.close()
            if (bitmap == null) return ""
            val scaled = if (bitmap.width > 1280) {
                val ratio = 1280f / bitmap.width
                Bitmap.createScaledBitmap(bitmap, 1280, (bitmap.height * ratio).toInt(), true)
            } else bitmap
            FileOutputStream(file).use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 75, out)
            }
            file.absolutePath
        } catch (e: Exception) {
            ""
        }
    }
}
