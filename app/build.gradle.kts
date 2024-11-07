plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("com.github.ben-manes.versions") version "0.46.0"
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

    // AndroidX Testing - JUnit4 test runner and Core Testing Library
    androidTestImplementation(libs.androidx.junit.v115)
    androidTestImplementation(libs.androidx.runner)
    androidTestImplementation(libs.core.ktx.v150)
    androidTestImplementation(libs.androidx.espresso.core.v351)

    testImplementation(libs.junit)
    testImplementation(libs.junit.v115)

}
