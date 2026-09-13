import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/**
 * Optional monetization module: RevenueCat (purchases) + future Firebase quota
 * backend client.
 *
 * This module is only included in the Gradle build when
 * `shweep.monetization.enabled=true` (see settings.gradle.kts). With the
 * default value it is never configured and none of its dependencies are
 * resolved, so the shipping app stays free of RevenueCat/Firebase.
 *
 * `composeApp` does NOT depend on this module yet. Wiring it in is a separate,
 * flag-gated step described in MONETIZATION.md.
 */
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Monetization"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.revenuecat.core)
            implementation(libs.revenuecat.result)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "com.skooldev.shweep.monetization"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
