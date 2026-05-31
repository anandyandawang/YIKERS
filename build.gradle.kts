// Root build. No code here. Subprojects pull shared repos + kotlin.
plugins {
    kotlin("jvm") version "2.1.0" apply false
}

allprojects {
    repositories {
        mavenCentral()
        maven(url = "https://oss.sonatype.org/content/repositories/snapshots/")
    }
}
