// e2e = cross-process socket tests: real server + real client over a loopback socket.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    testImplementation(project(":shared"))
    testImplementation(project(":server"))
    testImplementation(project(":client"))
    testImplementation(project(":bot"))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.gdx)
    testImplementation(libs.gdx.backend.headless)
    testRuntimeOnly(variantOf(libs.gdx.platform) { classifier("natives-desktop") })
    testRuntimeOnly(variantOf(libs.gdx.box2d.platform) { classifier("natives-desktop") })
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    testLogging { events("passed", "skipped", "failed") }
}
