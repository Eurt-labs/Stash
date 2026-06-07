import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.compose.compiler)
}

group = "com.example.stash"
version = "1.1.0"

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material)
    
    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.swing)

    // Networking
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)

    // ID3 tagging
    implementation(libs.mp3agic)
}

compose.desktop {
    application {
        mainClass = "com.example.stash.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Stash"
            packageVersion = "1.1.0"
            vendor = "Eurt-labs"
            copyright = "© 2026 Eurt-labs. All rights reserved."
            windows {
                dirChooser = true
                menu = true
                shortcut = true
            }
        }
    }
}

tasks.withType<org.jetbrains.compose.desktop.application.tasks.AbstractJPackageTask>().configureEach {
    freeArgs.add("--temp")
    freeArgs.add(project.layout.buildDirectory.dir("jpackage-temp").get().asFile.absolutePath)
    
    doFirst {
        val tempDir = project.file("build/jpackage-temp")
        if (tempDir.exists()) {
            tempDir.deleteRecursively()
        }
        
        val resourcesDir = project.file("build/compose/tmp/resources")
        if (!resourcesDir.exists()) {
            resourcesDir.mkdirs()
        }
        project.copy {
            from(project.file("../installer-resources"))
            into(resourcesDir)
        }
        println("Injected custom WiX resource files into: ${resourcesDir.absolutePath}")
    }
}