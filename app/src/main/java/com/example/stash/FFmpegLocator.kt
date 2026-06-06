package com.example.stash

import com.yausername.ffmpeg.FFmpeg
import android.util.Log

class FFmpegLocator {
    fun find() {
        val f = FFmpeg.getInstance()
        // Will throw compile error if these don't exist
        Log.d("FFMPEG", f.getBinPath() ?: "unknown")
    }
}
