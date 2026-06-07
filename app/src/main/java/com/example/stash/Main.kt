package com.example.stash

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.example.stash.ui.DesktopApp

fun main() {
    println("--- Running yt-dlp directly ---")
    val cmd = listOf(
        "yt-dlp",
        "--dump-json",
        "--no-download",
        "--no-warnings",
        "--flat-playlist",
        "https://www.youtube.com/playlist?list=PLxCzCOWd7aiGmXg4NoX6R31AsC5LeCPHe"
    )
    try {
        val process = ProcessBuilder(cmd)
            .redirectErrorStream(true)
            .start()
        
        process.inputStream.bufferedReader().useLines { lines ->
            lines.forEachIndexed { i, line ->
                println("LINE " + i + ": " + line.take(120))
            }
        }
        val exitCode = process.waitFor()
        println("Exit code: " + exitCode)
    } catch (e: Exception) {
        e.printStackTrace()
    }
    System.exit(0)
}

