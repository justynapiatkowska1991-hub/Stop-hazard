plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val useNetstack =
    (project.findProperty("stophazard.netstack") as String?)?.toBooleanStrictOrNull() ?: false
val enableNetstackRuntime =
    (project.findProperty("stophazard.enableNetstack") as String?)?.toBooleanStrictOrNull() ?: false

android {
    namespace = "pl.stophazard.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "pl.stophazard.app"
        // NetValve's production AAR requires API 29+. Keep the normal build at 26,
        // while the NetValve production candidate is built with the required floor.
        minSdk = if (useNetstack) 29 else 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        buildConfigField("boolean", "USE_NETSTACK", useNetstack.toString())
        buildConfigField("boolean", "ENABLE_NETSTACK_RUNTIME", enableNetstackRuntime.toString())
    }

    sourceSets {
        getByName("main") {
            if (useNetstack) {
                kotlin.srcDir("src/netstack/kotlin")
            }
        }
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    if (useNetstack) {
        implementation(files("libs/netstack.aar"))
    }
    testImplementation("junit:junit:4.13.2")
}
