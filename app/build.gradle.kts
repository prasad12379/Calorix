plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.run"
    compileSdk = 36                         // ← fixed: release(36) is not valid syntax

    defaultConfig {
        applicationId = "com.example.run"
        minSdk = 24
        targetSdk = 36                      // ← fixed: matched to compileSdk
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // ── All your original dependencies kept exactly ───────────────────────
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // ── Maps ─────────────────────────────────────────────────────────────
    implementation("org.osmdroid:osmdroid-android:6.1.16")
    implementation("org.maplibre.gl:android-sdk:11.8.0")     // ← kept once (was duplicated 3x)
    implementation("org.maplibre.gl:android-plugin-localization-v9:1.0.0")
    implementation("org.maplibre.gl:android-sdk-geojson:6.0.1")

    implementation("org.maplibre.gl:android-sdk-geojson:6.0.1")

    // ── Networking ───────────────────────────────────────────────────────
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // ── Coroutines ───────────────────────────────────────────────────────
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // ── Gson ─────────────────────────────────────────────────────────────
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // ── Lottie ───────────────────────────────────────────────────────────
    implementation("com.airbnb.android:lottie:6.1.0")

    // ── WorkManager ──────────────────────────────────────────────────────
    implementation("androidx.work:work-runtime:2.9.0")

    // ── Compose extras ───────────────────────────────────────────────────
    implementation("androidx.activity:activity-compose:1.9.3")          // ← fixed: 1.9.x → 1.9.3
    implementation("androidx.compose.material3:material3:1.3.1")        // ← fixed: 1.x.x → 1.3.1
    implementation("androidx.compose.animation:animation:1.7.5")
    implementation(libs.androidx.compose.ui.geometry)        // ← fixed: 1.x.x → 1.7.5

    // ── Testing ──────────────────────────────────────────────────────────
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("androidx.compose.material:material-icons-extended:1.6.7")

    implementation("androidx.work:work-runtime-ktx:2.9.0")
}