plugins {
    id("com.android.application")
    // Kotlin được tích hợp sẵn từ AGP 9.0 trở lên, không cần plugin org.jetbrains.kotlin.android
}

android {
    namespace = "com.min.assistanttile"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.min.assistanttile"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
}
