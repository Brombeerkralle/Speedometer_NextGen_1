plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("com.github.ben-manes.versions") version "0.46.0"
    id("org.jetbrains.kotlin.kapt")         // KAPT for annotation processing in Kotlin
    id("dagger.hilt.android.plugin")        // Hilt plugin for Dependency Injection
    id("com.google.dagger.hilt.android")    // Apply the Hilt plugin
}

android {
    namespace = "com.example.speedometer_nextgen_1"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.speedometer_nextgen_1"
        minSdk = 34  // Lowered for broader compatibility
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.foundation.android)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Additional dependencies
    implementation(libs.easypermissions)
    implementation(libs.play.services.location)
    implementation(libs.androidx.junit.ktx)

    implementation(libs.hilt.android)
    kapt(libs.hilt.android.compiler)

    // Optional - Hilt testing dependencies
    androidTestImplementation(libs.hilt.android.testing)
    kaptAndroidTest(libs.hilt.android.compiler)
    testImplementation(libs.hilt.android.testing)
    kaptTest(libs.hilt.android.compiler)
}
