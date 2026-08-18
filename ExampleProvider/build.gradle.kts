plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

val version = 1

cloudstream {
    description = "Internet Archive Movies Provider"
    authors = listOf("Cloudburst", "Luna712")
    status = 1
    tvTypes = listOf("Movie")
    requiresResources = false
    language = "en"
    iconUrl = "https://upload.wikimedia.org/wikipedia/commons/2/2f/Korduene_Logo.png"
}

android {
    defaultConfig {
        minSdk = 21
    }
    buildFeatures {
        buildConfig = true
    }
}