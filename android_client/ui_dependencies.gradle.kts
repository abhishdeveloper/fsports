// app/build.gradle.kts

android {
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // UI & Material Design
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // AndroidX Lifecycle, ViewModel, and Activity/Fragment KTX
    implementation("androidx.activity:activity-ktx:1.8.0")
    implementation("androidx.fragment:fragment-ktx:1.6.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")

    // Facebook Shimmer (for skeleton loading effects)
    implementation("com.facebook.shimmer:shimmer:0.5.0")

    // Coil (Coroutine Image Loader)
    implementation("io.coil-kt:coil:2.4.0")

    // Lottie (Premium Success Animations)
    implementation("com.airbnb.android:lottie:6.1.0")

    // Konfetti (Particle Explosions)
    implementation("nl.dionsegijn:konfetti-xml:3.0.1")
}