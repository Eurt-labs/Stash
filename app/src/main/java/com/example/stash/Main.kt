package com.example.stash

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.example.stash.ui.DesktopApp

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Stash"
    ) {
        val orchestrator = StashOrchestrator()
        DesktopApp(orchestrator)
    }
}
