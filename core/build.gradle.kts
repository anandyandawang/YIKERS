// Core = platform-agnostic game logic. No backend here.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(libs.gdx)
    implementation(libs.gdx.box2d)
    // Fleks = modern Kotlin ECS. Game sim live here.
    implementation(libs.fleks)
    // ktx = idiomatic Kotlin libGDX helpers.
    implementation(libs.ktx.app)
    implementation(libs.ktx.box2d)
    implementation(libs.ktx.graphics)
    implementation(libs.ktx.math)

    // --- headless integration test ---
    // Headless backend: boots Gdx env (app/files) + loads gdx native, no GL.
    testImplementation(libs.gdx.backend.headless)
    testImplementation(libs.junit.jupiter)
    // Desktop natives at test runtime: headless backend ships none, Box2D needs them.
    testRuntimeOnly(variantOf(libs.gdx.platform) { classifier("natives-desktop") })
    testRuntimeOnly(variantOf(libs.gdx.box2d.platform) { classifier("natives-desktop") })
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
    testLogging { events("passed", "skipped", "failed") }
}
