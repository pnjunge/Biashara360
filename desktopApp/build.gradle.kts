import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25)
        }
    }
    sourceSets {
        val desktopTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.sqldelight.sqlite.driver)
                implementation("io.ktor:ktor-client-mock:${libs.versions.ktor.get()}")
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation(project(":shared"))
                implementation(compose.desktop.currentOs)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(compose.components.resources)
                implementation(libs.kotlinx.coroutines.swing)
                // Material3 requires 0.7.x; shared and Ktor still use the legacy Clock/Instant classes.
                implementation("org.jetbrains.kotlinx:kotlinx-datetime") {
                    version { strictly("0.7.1-0.6.x-compat") }
                }
                implementation(libs.koin.core)
                implementation(libs.ktor.client.cio)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.client.logging)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.kotlinx.serialization.json)
                runtimeOnly("org.slf4j:slf4j-simple:1.7.36")
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.app.biashara.MainKt"
        jvmArgs += "--enable-native-access=ALL-UNNAMED"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Biashara360ERP"
            packageVersion = "1.0.0"
            description = "Biashara360ERP - Business Management for Kenyan Traders"
            copyright = "© 2025 Biashara360ERP"
            vendor = "Biashara360ERP"
            linux { iconFile.set(project.file("src/desktopMain/resources/icon.png")) }
            windows { iconFile.set(project.file("src/desktopMain/resources/icon.ico")) }
            macOS { iconFile.set(project.file("src/desktopMain/resources/icon.icns")) }
        }
    }
}
