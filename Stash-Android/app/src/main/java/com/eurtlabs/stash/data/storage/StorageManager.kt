package com.eurtlabs.stash.data.storage

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Environment
import androidx.documentfile.provider.DocumentFile
import java.io.File

object StorageManager {

    private const val PREFS_NAME = "stash_prefs"
    private const val KEY_FIRST_LAUNCH_DONE = "first_launch_done"
    private const val KEY_CUSTOM_STORAGE_URI = "custom_storage_uri"
    private const val KEY_CUSTOM_STORAGE_PATH = "custom_storage_path"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isFirstLaunch(context: Context): Boolean {
        return !getPrefs(context).getBoolean(KEY_FIRST_LAUNCH_DONE, false)
    }

    fun setFirstLaunchCompleted(context: Context) {
        getPrefs(context).edit().putBoolean(KEY_FIRST_LAUNCH_DONE, true).apply()
    }

    fun getCustomStorageUri(context: Context): String? {
        return getPrefs(context).getString(KEY_CUSTOM_STORAGE_URI, null)
    }

    fun setCustomStorage(context: Context, uri: Uri) {
        val displayPath = getPathFromUri(context, uri)
        getPrefs(context).edit()
            .putString(KEY_CUSTOM_STORAGE_URI, uri.toString())
            .putString(KEY_CUSTOM_STORAGE_PATH, displayPath)
            .putBoolean(KEY_FIRST_LAUNCH_DONE, true)
            .apply()
    }

    fun setUseDefaultStorage(context: Context) {
        getPrefs(context).edit()
            .remove(KEY_CUSTOM_STORAGE_URI)
            .remove(KEY_CUSTOM_STORAGE_PATH)
            .putBoolean(KEY_FIRST_LAUNCH_DONE, true)
            .apply()
    }

    fun getDisplayStoragePath(context: Context): String {
        val customPath = getPrefs(context).getString(KEY_CUSTOM_STORAGE_PATH, null)
        if (!customPath.isNullOrBlank()) {
            return customPath
        }
        return "Music / Stash"
    }

    fun getTargetOutputDir(context: Context): File {
        val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
            ?: File(context.filesDir, "Music")
        val stashDir = File(baseDir, "Stash")
        if (!stashDir.exists()) {
            stashDir.mkdirs()
        }
        return stashDir
    }

    private fun getPathFromUri(context: Context, uri: Uri): String {
        return try {
            val doc = DocumentFile.fromTreeUri(context, uri)
            doc?.name ?: uri.path ?: "Selected Folder"
        } catch (e: Exception) {
            uri.path ?: "Custom Folder"
        }
    }
}
