package com.mbwbill.app.util

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mbwbill.app.data.Customer
import java.io.InputStreamReader
import java.io.OutputStreamWriter

object StorageManager {
    private const val PREF_NAME = "app_prefs"
    private const val PREF_KEY = "selected_folder_uri"

    fun saveFolderUri(context: Context, uri: Uri) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString(PREF_KEY, uri.toString()).apply()
    }

    fun getFolderUri(context: Context): Uri? {
        val str = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(PREF_KEY, null)
        return if (str != null) Uri.parse(str) else null
    }

    fun getOrCreateAppFolder(context: Context): DocumentFile? {
        val uri = getFolderUri(context) ?: return null
        val root = DocumentFile.fromTreeUri(context, uri) ?: return null
        var appFolder = root.findFile("MBWBillApp")
        if (appFolder == null || !appFolder.isDirectory) {
            appFolder = root.createDirectory("MBWBillApp")
        }
        return appFolder
    }

    fun getImagesFolder(context: Context): DocumentFile? {
        val appFolder = getOrCreateAppFolder(context) ?: return null
        var imgFolder = appFolder.findFile("Images")
        if (imgFolder == null || !imgFolder.isDirectory) {
            imgFolder = appFolder.createDirectory("Images")
        }
        return imgFolder
    }

    // Naya function: Data ko JSON me save karna
    fun saveBackupJson(context: Context, customers: List<Customer>) {
        val appFolder = getOrCreateAppFolder(context) ?: return
        var jsonFile = appFolder.findFile("customers_backup.json")
        if (jsonFile == null) {
            jsonFile = appFolder.createFile("application/json", "customers_backup.json")
        }
        if (jsonFile != null) {
            try {
                val outputStream = context.contentResolver.openOutputStream(jsonFile.uri, "wt")
                if (outputStream != null) {
                    outputStream.use { os ->
                        val writer = OutputStreamWriter(os)
                        val json = Gson().toJson(customers)
                        writer.write(json)
                        writer.flush()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Naya function: JSON se data wapas padhna
    fun loadBackupJson(context: Context): List<Customer>? {
        val appFolder = getOrCreateAppFolder(context) ?: return null
        val jsonFile = appFolder.findFile("customers_backup.json") ?: return null
        return try {
            val inputStream = context.contentResolver.openInputStream(jsonFile.uri)
            if (inputStream != null) {
                inputStream.use { isStream ->
                    val reader = InputStreamReader(isStream)
                    val type = object : TypeToken<List<Customer>>() {}.type
                    val customers: List<Customer> = Gson().fromJson(reader, type)
                    customers
                }
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
