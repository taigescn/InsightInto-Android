import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.taiges.insight.into.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.taiges.insight.into.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 100000
        versionName = "1.0.0"
        multiDexEnabled = true

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = rootProject.file("./InsightIntoKeyStore")
            keyAlias = "InsightInto"
            storePassword = "insightinto"
            keyPassword = "insightinto"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
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

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    flavorDimensions += listOf("environment")
    productFlavors {
        val localProps = Properties()
        localProps.load(FileInputStream(rootProject.file("./local.properties")))
        create("envTest") {
            dimension = "environment"
            val terminalKey = localProps["TEST_TERMINAL_KEY"] ?: ""
            val apiBaseUrl = localProps["TEST_API_BASE_URL"] ?: ""
            //测试环境 TerminalKey
            buildConfigField("String", "TERMINAL_KEY", "\"$terminalKey\"")
            //测试环境 ApiBaseUrl
            buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
            manifestPlaceholders["appLabel"] = "InsightIntoTest"
        }

        create("envDev") {
            dimension = "environment"
            val terminalKey = localProps["DEV_TERMINAL_KEY"] ?: ""
            val apiBaseUrl = localProps["DEV_API_BASE_URL"] ?: ""
            buildConfigField("String", "TERMINAL_KEY", "\"$terminalKey\"")
            buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
            manifestPlaceholders["appLabel"] = "InsightIntoDev"
        }
    }
}

dependencies {

    implementation(project(":insight-base"))
    implementation(project(":core:insight-core"))
    debugImplementation(project(":module:insight-log-debug"))
    releaseImplementation(project(":module:insight-log-release"))
    implementation(libs.multidex)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.gson)
    implementation(libs.googleAdid)
    implementation(libs.googleLocation)
    implementation(libs.baseRecyclerViewAdapterHelper)
    implementation(libs.flexibledivider)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}