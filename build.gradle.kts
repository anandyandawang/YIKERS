// Root build. No code here. Subprojects pull shared repos + kotlin.
// The RoboVM gradle plugin has no plugins{} marker, so the ios module
// applies it the legacy way; its classpath is declared here.
buildscript {
    repositories {
        mavenCentral()
        maven(url = "https://oss.sonatype.org/content/repositories/snapshots/")
    }
    dependencies {
        classpath("com.mobidevelop.robovm:robovm-gradle-plugin:2.3.21")
    }
}

plugins {
    kotlin("jvm") version "2.3.21" apply false
}

allprojects {
    repositories {
        mavenCentral()
        maven(url = "https://oss.sonatype.org/content/repositories/snapshots/")
    }
}
