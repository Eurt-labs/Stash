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
            val doc = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, uri)
            doc?.name ?: uri.lastPathSegment?.substringAfterLast(":") ?: "Custom Folder"
        } catch (e: Exception) {
            uri.lastPathSegment?.substringAfterLast(":") ?: "Custom Folder"
        }
    }

    fun copyToCustomStorage(context: Context, sourceFile: File, subfolderName: String? = null): Boolean {
        val customUriStr = getCustomStorageUri(context)
        if (customUriStr.isNullOrBlank()) return false
        
        try {
            val treeUri = Uri.parse(customUriStr)
            var docFile = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, treeUri) ?: return false
            
            // If subfolder is provided, find or create it
            if (!subfolderName.isNullOrBlank()) {
                val subfolder = docFile.findFile(subfolderName) ?: docFile.createDirectory(subfolderName)
                if (subfolder != null) {
                    docFile = subfolder
                }
            }
            
            // Check if file already exists in the SAF directory and delete it to overwrite
            val existingFile = docFile.findFile(sourceFile.name)
            existingFile?.delete()
            
            val mimeType = if (sourceFile.extension.equals("mp4", true) || sourceFile.extension.equals("mkv", true) || sourceFile.extension.equals("webm", true)) {
                "video/${sourceFile.extension}"
            } else {
                "audio/${sourceFile.extension}"
            }
            
            val newFile = docFile.createFile(mimeType, sourceFile.name) ?: return false
            
            context.contentResolver.openOutputStream(newFile.uri)?.use { outStream ->
                sourceFile.inputStream().use { inStream ->
                    inStream.copyTo(outStream)
                }
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun deleteFromCustomStorage(context: Context, fileName: String, subfolderName: String? = null): Boolean {
        val customUriStr = getCustomStorageUri(context)
        if (customUriStr.isNullOrBlank()) return false
        
        try {
            val treeUri = Uri.parse(customUriStr)
            var docFile = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, treeUri) ?: return false
            
            if (!subfolderName.isNullOrBlank()) {
                val subfolder = docFile.findFile(subfolderName)
                if (subfolder != null) {
                    docFile = subfolder
                }
            }
            
            val existingFile = docFile.findFile(fileName)
            return existingFile?.delete() ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}
