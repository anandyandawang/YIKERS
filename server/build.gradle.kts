// Server = the headless authoritative sim (Fleks + Box2D + RNG). No gdx-graphics /
// GL: the world runs without a screen, the client renders from snapshots. The
// integration tests (the real sim seam CI reuses) live here.
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
    // GameInstance.createWorld needs it. integrationTest inherits these via
    // extendsFrom(implementation/runtimeOnly), so its old explicit lines are gone.
    implementation(libs.gdx.backend.headless)
    runtimeOnly(variantOf(libs.gdx.platform) { classifier("natives-desktop") })
    runtimeOnly(variantOf(libs.gdx.box2d.platform) { classifier("natives-desktop") })
}

// --- integration tests -------------------------------------------------------
// Headless sim tests (Fleks + Box2D, no GL) live in their own source set so the
// default `test` set stays free for plain unit tests. The sim is the real
// integration seam CI + an eventual iOS build reuse.
sourceSets {
    create("integrationTest") {
        compileClasspath += sourceSets["main"].output
        runtimeClasspath += sourceSets["main"].output
    }
}

// The integration config tree mirrors main's, so tests see all game deps.
configurations["integrationTestImplementation"].extendsFrom(configurations["implementation"])
configurations["integrationTestRuntimeOnly"].extendsFrom(configurations["runtimeOnly"])

dependencies {
    // Headless backend + desktop natives are inherited from main now (the server
    // runs headless in production too), so only the JUnit bits are test-only.
    "integrationTestImplementation"(libs.junit.jupiter)
    "integrationTestRuntimeOnly"(libs.junit.platform.launcher)
}

val integrationTest = tasks.register<Test>("integrationTest") {
    description = "Runs headless Fleks + Box2D integration tests."
    group = "verification"
    testClassesDirs = sourceSets["integrationTest"].output.classesDirs
    classpath = sourceSets["integrationTest"].runtimeClasspath
    useJUnitPlatform()
    testLogging { events("passed", "skipped", "failed") }
    shouldRunAfter(tasks.named("test"))
}

tasks.named("check") { dependsOn(integrationTest) }

// `test` source set kept as the unit-test seam (none yet).
tasks.named<Test>("test") {
    useJUnitPlatform()
}
