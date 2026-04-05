import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvm("desktop")
    sourceSets {
        val desktopMain by getting {
            dependencies {
                implementation(project(":shared"))
                implementation(compose.desktop.currentOs)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(compose.components.resources)
                implementation(libs.kotlinx.coroutines.swing)
                implementation(libs.koin.core)
                implementation(libs.ktor.client.java)
                implementation(libs.kotlinx.serialization.json)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.app.biashara.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Biashara360ERP"
            packageVersion = "1.0.0"
            description = "Biashara360ERP - Business Management for Kenyan Traders"
            copyright = "© 2025 Biashara360ERP"
            vendor = "Biashara360ERP"
            linux { iconFile.set(project.file("src/main/resources/icon.png")) }
            windows { iconFile.set(project.file("src/main/resources/icon.ico")) }
            macOS { iconFile.set(project.file("src/main/resources/icon.icns")) }
        }
    }
}
