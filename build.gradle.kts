// Root build. No code here. Subprojects pull shared repos + kotlin.
// The RoboVM gradle plugin has no plugins{} marker, so the ios module
// applies it the legacy way; its classpath is declared here.
buildscript {
    repositories {
        mavenCentral()
        maven(url = "https://oss.sonatype.org/content/repositories/snapshots/")
    }
    dependencies {
        // Kept as a literal: version-catalog (libs.*) accessors are not
        // available inside a buildscript{} block, since this classpath is
        // compiled before the catalog exists.
        classpath("com.mobidevelop.robovm:robovm-gradle-plugin:2.3.21")
    }
}

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
}

allprojects {
    repositories {
        mavenCentral()
        maven(url = "https://oss.sonatype.org/content/repositories/snapshots/")
    }
}
