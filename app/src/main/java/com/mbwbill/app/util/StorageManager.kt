package com.mbwbill.app.util

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

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
}
