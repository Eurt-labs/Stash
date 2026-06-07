package com.example.stash

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.example.stash.ui.DesktopApp

fun main() = application {
    try {
        javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName())
    } catch (e: Exception) {
        // Ignore
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Stash"
    ) {
        val orchestrator = StashOrchestrator()
        DesktopApp(orchestrator)
    }
}
