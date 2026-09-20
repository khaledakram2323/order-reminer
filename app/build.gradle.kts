plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.services)
}

android {

    namespace = "com.example.orderreminders"

    compileSdk = 35

    defaultConfig {

        applicationId = "com.example.orderreminders"

        minSdk = 24

        targetSdk = 34

        versionCode = 1

        versionName = "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {

        release {

            isMinifyEnabled = false

            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    // AndroidX
    implementation(
        libs.androidx.core.ktx
    )

    implementation(
        libs.androidx.appcompat
    )

    implementation(
        libs.androidx.activity
    )

    implementation(
        libs.androidx.constraintlayout
    )

    // Material
    implementation(
        libs.material
    )

    // RecyclerView
    implementation(
        libs.androidx.recyclerview
    )

    // Lifecycle
    implementation(
        libs.androidx.lifecycle.viewmodel
    )

    implementation(
        libs.androidx.lifecycle.livedata
    )

    implementation(
        libs.androidx.lifecycle.runtime
    )

    // WorkManager
    implementation(
        libs.androidx.work.runtime
    )

    // Room
    // implementation(libs.androidx.room.runtime)
    // implementation(libs.androidx.room.ktx)
    // ksp(libs.androidx.room.compiler)

    // Coroutines
    implementation(
        libs.kotlinx.coroutines.android
    )

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.auth.ktx)

    // Tests
    testImplementation(
        libs.junit
    )

    androidTestImplementation(
        libs.androidx.junit
    )

    androidTestImplementation(
        libs.androidx.espresso.core
    )
}