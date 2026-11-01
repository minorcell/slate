import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("keystore.properties")
if (keystorePropertiesFile.exists()) {
    keystorePropertiesFile.inputStream().use { keystoreProperties.load(it) }
}

android {
    namespace = "dev.minorcell.slate"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.minorcell.slate"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
    }

    val storePath = keystoreProperties.getProperty("storeFile")?.takeIf { it.isNotBlank() }
    val storePassword = keystoreProperties.getProperty("storePassword")?.takeIf { it.isNotBlank() }
    val keyAlias = keystoreProperties.getProperty("keyAlias")?.takeIf { it.isNotBlank() }
    val keyPassword = keystoreProperties.getProperty("keyPassword")?.takeIf { it.isNotBlank() }
    if (storePath != null && storePassword != null && keyAlias != null && keyPassword != null) {
        signingConfigs {
            create("release") {
                storeFile = rootProject.file(storePath)
                this.storePassword = storePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.findByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    packaging {
        resources {
            excludes += "DebugProbesKt.bin"
            excludes += "kotlin-tooling-metadata.json"
            excludes += "kotlin/collections/*.kotlin_builtins"
            excludes += "kotlin/ranges/*.kotlin_builtins"
            excludes += "kotlin/reflect/*.kotlin_builtins"
            excludes += "kotlin/concurrent/**/*.kotlin_builtins"
            excludes += "kotlin/annotation/*.kotlin_builtins"
            excludes += "kotlin/internal/*.kotlin_builtins"
            excludes += "kotlin/coroutines/*.kotlin_builtins"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit)
}
