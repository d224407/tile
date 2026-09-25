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

    signingConfigs {
        // Chỉ tạo config này khi đủ 4 biến môi trường (do workflow release set ra).
        // Build debug local bình thường không có các biến này -> bỏ qua, không lỗi.
        val storeFile = System.getenv("RELEASE_STORE_FILE")
        val storePassword = System.getenv("RELEASE_STORE_PASSWORD")
        val keyAlias = System.getenv("RELEASE_KEY_ALIAS")
        val keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
        if (!storeFile.isNullOrBlank() && !storePassword.isNullOrBlank() &&
            !keyAlias.isNullOrBlank() && !keyPassword.isNullOrBlank()
        ) {
            create("release") {
                this.storeFile = file(storeFile)
                this.storePassword = storePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfigs.findByName("release")?.let {
                signingConfig = it
            }
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
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")
}
