rootProject.name = "Shweep"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

include(":composeApp")

// The :monetization module (RevenueCat + Firebase quota backend) is only part
// of the build when explicitly enabled. With the default `false` value it is
// never configured, so no RevenueCat/Firebase dependency is resolved and
// nothing from it can reach the shipping app.
val monetizationEnabled = providers.gradleProperty("shweep.monetization.enabled")
    .map { it.equals("true", ignoreCase = true) }
    .getOrElse(false)

if (monetizationEnabled) {
    include(":monetization")
}