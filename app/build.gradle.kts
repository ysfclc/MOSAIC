plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val mosaicDebugKeystorePath = providers.environmentVariable("MOSAIC_DEBUG_KEYSTORE").orNull
val mosaicCiVersionCode = maxOf(2, providers.gradleProperty("mosaicVersionCode").orNull?.toIntOrNull() ?: 2)

android {
    namespace = "com.ysfclc.mosaic"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.ysfclc.mosaic"
        minSdk = 26
        targetSdk = 36
        versionCode = mosaicCiVersionCode
        versionName = "0.2.0-browser-preview"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (!mosaicDebugKeystorePath.isNullOrBlank()) {
            create("stableDebug") {
                storeFile = file(mosaicDebugKeystorePath)
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        getByName("debug") {
            if (!mosaicDebugKeystorePath.isNullOrBlank()) {
                signingConfig = signingConfigs.getByName("stableDebug")
            }
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

dependencies {
    implementation(project(":core:media"))
    implementation(project(":core:library"))
    implementation(project(":core:design"))
    implementation(project(":core:persistence"))

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation("androidx.media3:media3-ui:1.11.1")

    implementation(platform("androidx.compose:compose-bom:2026.08.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("org.robolectric:robolectric:4.15.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2026.08.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
}
