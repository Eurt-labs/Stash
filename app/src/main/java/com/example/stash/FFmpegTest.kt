package com.example.stash

import com.yausername.ffmpeg.FFmpeg

class FFmpegTest {
    fun test() {
        val f = FFmpeg.getInstance()
        // We want to see what methods f has. We'll intentionally cause a compile error by calling a fake method.
        f.fakeMethodThatDoesNotExist()
    }
}
