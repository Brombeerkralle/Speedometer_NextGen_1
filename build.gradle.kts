// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false// Should map to com.android.application with AGP version 8.7.2
    alias(libs.plugins.jetbrains.kotlin.android) apply false// Should map to org.jetbrains.kotlin.android with Kotlin version 1.9.10
}