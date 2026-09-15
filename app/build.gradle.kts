import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    kotlin("plugin.serialization") version "2.1.0"
}

android {
    namespace = "com.hkm.pozix"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.hkm.pozix"
        minSdk = 24
        targetSdk = 36
        versionCode = 34
        versionName = "1.8.9"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val localProperties = Properties()
            val localPropertiesFile = rootProject.file("local.properties")
            if (localPropertiesFile.exists()) {
                localPropertiesFile.inputStream().use { localProperties.load(it) }
            }
            val keystorePath = localProperties.getProperty("KEYSTORE_FILE") ?: System.getenv("KEYSTORE_FILE")
            val storePass = localProperties.getProperty("KEYSTORE_PASSWORD") ?: System.getenv("KEYSTORE_PASSWORD")
            val alias = localProperties.getProperty("KEY_ALIAS") ?: System.getenv("KEY_ALIAS")
            val keyPass = localProperties.getProperty("KEY_PASSWORD") ?: System.getenv("KEY_PASSWORD")
            if (keystorePath != null && file(keystorePath).exists() && storePass != null) {
                storeFile = file(keystorePath)
                storePassword = storePass
                keyAlias = alias
                keyPassword = keyPass
            }
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("release")
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
    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended")
    
    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.5")
    
    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    
    // Kotlinx Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    
    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    
    // OkHttp for Gemini API calls
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Typed quiz visuals: remote/local images rendered without WebView.
    // Coil 3.3.0 is the latest line compiled for Kotlin 2.2, this app's
    // current toolchain. Newer Coil releases require Kotlin 2.4 and would
    // upgrade the whole Compose graph unexpectedly.
    implementation("io.coil-kt.coil3:coil-compose:3.3.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.3.0")
    
    testImplementation(libs.junit)
    testImplementation("net.sf.kxml:kxml2:2.3.0")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
