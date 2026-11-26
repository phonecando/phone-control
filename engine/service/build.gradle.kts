plugins {
    id("com.android.library")
    kotlin("android")
    `maven-publish`
}

android {
    namespace = "com.phonecando.pce.service"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    publishing {
        singleVariant("release") {}
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    api(project(":api"))
    api(project(":core"))
    api(project(":gates"))
    implementation(project(":json"))
    implementation(project(":android"))
    implementation(project(":keystore"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                artifactId = "pce-service"
            }
        }
    }
}
