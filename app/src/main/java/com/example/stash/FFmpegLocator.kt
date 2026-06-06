package com.example.stash

import android.content.Context
import java.io.File

class FFmpegLocator {
    fun find(context: Context): String? {
        val noBackup = context.noBackupFilesDir
        val files = noBackup.walkTopDown().toList()
        for (f in files) {
            if (f.name == "ffmpeg" || f.name == "libffmpeg.so") {
                return f.absolutePath
            }
        }
        return null
    }
}
