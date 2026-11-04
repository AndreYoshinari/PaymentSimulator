plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
}

android {
    namespace = "br.com.paymentsimulator.data"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.turbine)
}
