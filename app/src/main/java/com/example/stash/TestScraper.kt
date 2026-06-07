package com.example.stash

import com.example.stash.parser.LinkParser
import kotlinx.coroutines.runBlocking
import java.io.File

fun main() {
    runBlocking {
        val orchestrator = StashOrchestrator()
        println("Resolving Taylor Swift...")
        try {
            val tracks = orchestrator.processLink("https://music.youtube.com/@TaylorSwift")
            println("Successfully resolved! Tracks found: ${tracks.size}")
            for ((idx, track) in tracks.take(5).withIndex()) {
                println("Track ${idx + 1}: ${track.title} by ${track.artists}")
            }
        } catch (e: Exception) {
            println("Exception caught:")
            e.printStackTrace()
        }
    }
}
