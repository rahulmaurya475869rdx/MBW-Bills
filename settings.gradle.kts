pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // JitPack repository add karna zaroori hai uCrop ke liye
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "MBWBill"
include(":app")
