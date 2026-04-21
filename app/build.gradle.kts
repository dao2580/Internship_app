/*
 * Copyright (c) 2025 Android project OpenVision API
 * All rights reserved.
 * Project: My Application
 * File: build.gradle.kts
 * Last Modified: 17/10/2025 2:22
 */

import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "vn.edu.usth.myapplication"
    compileSdk = 36

    defaultConfig {
        applicationId = "vn.edu.usth.myapplication"
        minSdk = 24
        targetSdk = 36
        versionCode = 5
        versionName = "1.5"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val properties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            properties.load(FileInputStream(localPropertiesFile))
        }

        buildFeatures {
            buildConfig = true
            viewBinding = true
        }

        buildConfigField(
            "String", "AZURE_TRANSLATOR_KEY", "\"${properties.getProperty("AZURE_TRANSLATOR_KEY", "")}\""
        )
        buildConfigField(
            "String", "AZURE_TRANSLATOR_REGION", "\"${properties.getProperty("AZURE_TRANSLATOR_REGION", "")}\""
        )
    }

    androidResources {
        noCompress += "tflite"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // Navigation components
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)

    // CameraX dependencies for embedded camera
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)

    // TensorFlow Lite for object detection
    implementation("com.google.ai.edge.litert:litert:1.4.1")

    // Azure Cognitive Services Translator
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)
    implementation(libs.androidx.navigation.navigation.ui)

    // ML KIT
    implementation("com.google.mlkit:translate:17.0.3")

    // Test dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")

// ViewModel + LiveData
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata:2.7.0")

// ViewPager2
    implementation("androidx.viewpager2:viewpager2:1.1.0")

}