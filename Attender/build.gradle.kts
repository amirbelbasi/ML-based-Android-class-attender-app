// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    //id("com.android.application") version "8.5.0" apply false
    //id("com.android.library") version "8.5.0" apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    id("com.chaquo.python") version "15.0.1" apply false
}