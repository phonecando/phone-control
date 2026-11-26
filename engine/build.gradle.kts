plugins {
    // Versions in gradle/libs.versions.toml, which consumer-sample shares.
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.android.library) apply false
}

allprojects {
    group = "com.phonecando.pce"
    version = "0.1.0-dev"
}
