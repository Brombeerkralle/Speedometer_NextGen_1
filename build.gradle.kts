// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    repositories {
        google()
        mavenCentral()
    }

    dependencies {

        //classpath(libs.gradle)
        //classpath(libs.kotlin.gradle.plugin)
        classpath(libs.hilt.android.gradle.plugin) // Hilt plugin

    }
}


plugins {
    alias(libs.plugins.android.application) // Should map to com.android.application with AGP version 8.7.2
    alias(libs.plugins.jetbrains.kotlin.android) // Should map to org.jetbrains.kotlin.android with Kotlin version 1.9.10
    alias(libs.plugins.hilt) apply false // Should map to com.google.dagger.hilt.android with Hilt version 2.44
    alias(libs.plugins.ksp) //  Should map to com.google.devtools.ksp
}