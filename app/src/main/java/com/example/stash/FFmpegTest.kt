package com.example.stash

import android.util.Log
import com.yausername.ffmpeg.FFmpeg
import java.lang.reflect.Modifier

class FFmpegTest {
    fun test() {
        val f = FFmpeg.getInstance()
        val methods = f.javaClass.declaredMethods
        for (m in methods) {
            Log.d("FFMPEG_TEST", "Method: ${Modifier.toString(m.modifiers)} ${m.returnType.name} ${m.name}(${m.parameterTypes.joinToString { it.name }})")
        }
        throw RuntimeException("STOP")
    }
}
