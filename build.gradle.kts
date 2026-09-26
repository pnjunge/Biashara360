plugins {
    alias(libs.plugins.kotlinMultiplatform).apply(false)
    alias(libs.plugins.kotlinJvm).apply(false)
    alias(libs.plugins.composeMultiplatform).apply(false)
    alias(libs.plugins.androidApplication).apply(false)
    alias(libs.plugins.androidLibrary).apply(false)
    alias(libs.plugins.kotlinSerialization).apply(false)
    alias(libs.plugins.sqlDelight).apply(false)
    alias(libs.plugins.ktor).apply(false)
}

// The Android modules set their own compileSdk in their individual build files.
// Removing a global afterEvaluate that touched Android extensions to avoid
// early configuration side-effects which can lead to "configuration resolved"
// timing issues with dependency alignment.

// Lightweight database for the portal-order concurrency and tenant-isolation tests.
project(":backend") {
    plugins.withId("org.jetbrains.kotlin.jvm") {
        dependencies.add("testRuntimeOnly", "com.h2database:h2:2.2.224")
    }
}
