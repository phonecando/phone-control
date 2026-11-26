plugins {
    // The engine's Kotlin (../gradle/libs.versions.toml): the published
    // artifacts' metadata needs a compiler at least as new as theirs.
    alias(libs.plugins.kotlin.jvm) apply false
}

allprojects {
    group = "com.phonecando.pce.consumer"
    version = "0.1.0"
}
