package com.example.stash

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.example.stash.ui.DesktopApp

fun main() {
    val orchestrator = StashOrchestrator()
    val url = "https://youtube.com/playlist?list=PLxCzCOWd7aiGmXg4NoX6R31AsC5LeCPHe&si=PCfnIiof2ruPkU0D"
    val parsed = orchestrator.validateLink(url)
    println("Parsed link: ${'$'}parsed")
    if (parsed == null) {
        println("Parsing failed!")
        return
    }
    
    kotlinx.coroutines.runBlocking {
        try {
            val tracks = orchestrator.processLink(url)
            println("Fetched ${'$'}{tracks.size} tracks successfully!")
            tracks.forEachIndexed { i, track ->
                println("${'$'}i: ${'$'}{track.title} - ${'$'}{track.artists} - Album: ${'$'}{track.album}")
            }
        } catch (e: Exception) {
            println("Exception occurred:")
            e.printStackTrace()
        }
    }
    System.exit(0)
}

