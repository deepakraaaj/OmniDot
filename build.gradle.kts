buildscript {
    dependencies {
        classpath("com.android.tools.build:gradle:8.12.0")
    }
}

plugins {
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

