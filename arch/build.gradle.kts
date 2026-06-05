// Arch = static architecture tests only (Konsist). No production code. Konsist
// reads every module's Kotlin source straight off disk, so this module needs NO
// project(...) deps on the modules it checks — a stray cross-module import just
// fails a test here.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    testImplementation(libs.konsist)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    testLogging { events("passed", "skipped", "failed") }
}
