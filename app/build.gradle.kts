plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "mv.legacy.link"
    compileSdk = 36

    defaultConfig {
        applicationId = "mv.legacy.link"
        minSdk = 26
        targetSdk = 36
        versionCode = 6
        versionName = "2.1.0"
    }

    buildFeatures { buildConfig = true }
    flavorDimensions += "distribution"
    productFlavors {
        create("personal") {
            dimension = "distribution"
            buildConfigField("boolean", "ALLOW_CLEARTEXT", "true")
            manifestPlaceholders["allowCleartextTraffic"] = "true"
        }
        create("play") {
            dimension = "distribution"
            buildConfigField("boolean", "ALLOW_CLEARTEXT", "false")
            manifestPlaceholders["allowCleartextTraffic"] = "false"
        }
    }

    val uploadStore = System.getenv("LEGACY_UPLOAD_STORE")
    if (!uploadStore.isNullOrBlank()) {
        signingConfigs.create("upload") {
            storeFile = file(uploadStore)
            storePassword = System.getenv("LEGACY_UPLOAD_STORE_PASSWORD")
            keyAlias = System.getenv("LEGACY_UPLOAD_ALIAS") ?: "legacy-link-upload"
            keyPassword = System.getenv("LEGACY_UPLOAD_KEY_PASSWORD")
            storeType = "PKCS12"
        }
    }

    buildTypes {
        release {
            if (!uploadStore.isNullOrBlank()) signingConfig = signingConfigs.getByName("upload")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-ktx:1.9.1")
    // QR pairing scanner
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
}
