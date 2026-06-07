package com.example.stash.util

import java.io.File

object DependencyResolver {
    /**
     * Resolves the path to a bundled dependency executable (yt-dlp, ffmpeg, ffprobe).
     * If the bundled executable is found (either in packaged app resources or in local dev folders),
     * returns its absolute path. Otherwise, returns the command name to fallback to system PATH.
     */
    fun resolveExecutable(name: String): String {
        val os = System.getProperty("os.name").lowercase()
        val extension = if (os.contains("win")) ".exe" else ""
        
        // 1. Check compose application resources dir (when packaged via jpackage/MSI)
        val resourcesDir = System.getProperty("compose.application.resources.dir")
        if (resourcesDir != null) {
            val file = File(resourcesDir, "windows/$name$extension")
            if (file.exists()) {
                return file.absolutePath
            }
        }
        
        // 2. Fallback check for development mode
        val devFile1 = File("../app-resources/windows/$name$extension")
        if (devFile1.exists()) {
            return devFile1.absolutePath
        }
        val devFile2 = File("app-resources/windows/$name$extension")
        if (devFile2.exists()) {
            return devFile2.absolutePath
        }

        // 3. Fallback to system command
        return name
    }

    /**
     * Checks if the executable is available and can be run.
     */
    fun checkExecutable(name: String, arg: String): Boolean {
        val resolved = resolveExecutable(name)
        return try {
            val process = ProcessBuilder(resolved, arg)
                .redirectErrorStream(true)
                .start()
            
            // Read output to avoid hang or to make sure it runs
            process.inputStream.bufferedReader().use { reader ->
                reader.readLine()
            }
            process.destroy()
            true
        } catch (e: Exception) {
            false
        }
    }
}
