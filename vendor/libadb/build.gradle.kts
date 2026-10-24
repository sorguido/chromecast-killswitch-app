plugins { id("com.android.library") }

android {
    namespace = "io.github.muntashirakon.adb"
    compileSdk = 36
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencyLocking { lockAllConfigurations() }

dependencies {
    implementation("androidx.annotation:annotation:1.9.1")
    implementation("org.bouncycastle:bcprov-jdk15to18:1.81")
    val replacementSpake2 = providers.gradleProperty("spake2Aar").orNull
    if (replacementSpake2 == null) {
        implementation("com.github.MuntashirAkon.spake2-java:spake2-android:2.2.1")
    } else {
        implementation(files(rootProject.file(replacementSpake2)))
    }
}
