// Server = the headless authoritative sim (Fleks + Box2D + RNG). No gdx-graphics /
// GL: the world runs without a screen, the client renders from snapshots. The
// unit + component sim tests (the real sim seam CI reuses) live here.
plugins {
    alias(libs.plugins.kotlin.jvm)
    // CBOR codec for the wire types shared with the client over the LAN socket.
    alias(libs.plugins.kotlin.serialization)
    // `application` = a runnable standalone server: ./gradlew :server:run.
    application
}

application {
    // Kotlin file-level main `fun main` in server/Main.kt -> com.yikers.server.MainKt.
    mainClass.set("com.yikers.server.MainKt")
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.gdx)
    implementation(libs.gdx.box2d)
    // Fleks = modern Kotlin ECS. Game sim live here.
    implementation(libs.fleks)
    // ktx = idiomatic Kotlin libGDX helpers (Box2D + math only; no graphics).
    implementation(libs.ktx.box2d)
    implementation(libs.ktx.math)
    // CBOR runtime for DedicatedServer / NetworkHost wire I/O.
    implementation(libs.kotlinx.serialization.cbor)
    // Headless backend + desktop natives are now MAIN runtime deps (not just test)
    // so `:server:run` can boot a HeadlessApplication and load the Box2D native —
    // GameInstance.createWorld needs it. The test tiers inherit these via the
    // main classpath, so they need no explicit headless/native lines.
    implementation(libs.gdx.backend.headless)
    runtimeOnly(variantOf(libs.gdx.platform) { classifier("natives-desktop") })
    runtimeOnly(variantOf(libs.gdx.box2d.platform) { classifier("natives-desktop") })
}

// --- test tiers --------------------------------------------------------------
// One headless `test` set (Fleks + Box2D, no GL), split by package: `unit` =
// one system, hand-poked; `component` = whole-sim-in-process, the full system
// list driven end to end. Both are fast, so one source set + one task. The real
// black-box integration layer (server + socket + real bot) lives in :e2e.
// Game deps come from main via testImplementation; only JUnit is test-only.
dependencies {
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    testLogging { events("passed", "skipped", "failed") }
}
